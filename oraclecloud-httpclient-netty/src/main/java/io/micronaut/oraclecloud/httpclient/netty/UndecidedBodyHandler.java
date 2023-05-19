/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handler that buffers some input data until the user decides whether they want it all buffered or as a stream. After
 * that, handling is delegated to {@link StreamReadingHandler} or {@link BufferFutureHandler}.
 */
final class UndecidedBodyHandler extends ChannelInboundHandlerAdapter {
    private final Runnable release;
    private final ByteBufAllocator alloc;
    private ChannelHandlerContext context;
    private List<HttpContent> buffer;
    private Throwable failure;

    private boolean decided = false;
    private boolean removed = false;

    UndecidedBodyHandler(Runnable release, ByteBufAllocator alloc) {
        this.release = release;
        this.alloc = alloc;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
        this.buffer = new ArrayList<>();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        removed = true;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpContent && buffer != null) {
            buffer.add((HttpContent) msg);
            if (msg instanceof LastHttpContent) {
                ctx.pipeline().remove(this);
                release.run();
            }
        } else {
            context.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (failure == null) {
            failure = cause;
        } else {
            ctx.fireExceptionCaught(cause);
        }
    }

    public boolean hasDecided() {
        return decided;
    }

    public void discard() {
        replaceWithHandler(DiscardingHandler.INSTANCE);
    }

    public CompletableFuture<InputStream> asInputStream() {
        StreamReadingHandler streamReadingHandler = new StreamReadingHandler(alloc);
        if (context.executor().inEventLoop()) {
            replaceWithHandler(streamReadingHandler);
            try {
                return CompletableFuture.completedFuture(streamReadingHandler.getInputStream());
            } catch (Throwable e) {
                CompletableFuture<InputStream> cf = new CompletableFuture<>();
                cf.completeExceptionally(e);
                return cf;
            }
        } else {
            Future<?> addFuture = context.executor().submit(() -> replaceWithHandler(streamReadingHandler));
            CompletableFuture<InputStream> streamFuture = new CompletableFuture<>();
            addFuture.addListener(future -> {
                if (future.isSuccess()) {
                    InputStream stream;
                    try {
                        stream = streamReadingHandler.getInputStream();
                    } catch (Throwable e) {
                        streamFuture.completeExceptionally(e);
                        return;
                    }
                    streamFuture.complete(stream);
                } else {
                    streamFuture.completeExceptionally(future.cause());
                }
            });
            return streamFuture;
        }
    }

    private void replaceWithHandler(DecidedBodyHandler handler) {
        if (decided) {
            throw new IllegalStateException("Already replaced");
        }
        decided = true;

        if (context.executor().inEventLoop()) {
            replaceWithHandler0(handler);
        } else {
            context.executor().submit(() -> replaceWithHandler0(handler));
        }
    }

    public CompletableFuture<ByteBuf> asBuffer() {
        BufferFutureHandler futureHandler = new BufferFutureHandler(alloc);
        replaceWithHandler(futureHandler);
        return futureHandler.future;
    }

    private void replaceWithHandler0(DecidedBodyHandler handler) {
        if (failure != null) {
            handler.onError(failure);
            if (buffer != null) {
                for (HttpContent httpContent : buffer) {
                    httpContent.release();
                }
            }
            return;
        }
        if (buffer != null) {
            for (HttpContent message : buffer) {
                handler.onContent(message);
            }
            buffer = null;
        }
        if (removed) {
            handler.onCancel();
        } else {
            context.pipeline()
                    .addAfter(context.name(), null, handler.new HandlerImpl(release))
                    .remove(this);
        }
    }
}
