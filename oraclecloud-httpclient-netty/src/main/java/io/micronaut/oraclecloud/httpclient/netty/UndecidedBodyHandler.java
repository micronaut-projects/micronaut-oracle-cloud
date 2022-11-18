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
    private ChannelHandlerContext context;
    private List<HttpContent> buffer;
    private Throwable failure;

    private boolean decided = false;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
        this.buffer = new ArrayList<>();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (failure != null) {
            ctx.fireExceptionCaught(failure);
        }
        if (buffer != null) {
            for (HttpContent message : buffer) {
                ctx.fireChannelRead(message);
            }
            buffer = null;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpContent && buffer != null) {
            buffer.add((HttpContent) msg);
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
        if (decided) {
            throw new IllegalStateException("Already replaced");
        }
        decided = true;

        context.executor().execute(() -> {
            context.pipeline()
                    .addAfter(context.name(), null, DiscardingHandler.INSTANCE)
                    .remove(this);
            context.read();
        });
    }

    public CompletableFuture<InputStream> asInputStream() {
        if (decided) {
            throw new IllegalStateException("Already replaced");
        }
        decided = true;

        StreamReadingHandler streamReadingHandler = new StreamReadingHandler();
        Future<?> addFuture = context.executor().submit(() -> {
            context.pipeline()
                    .addAfter(context.name(), null, streamReadingHandler)
                    .addAfter(context.name(), null, new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof HttpContent) {
                                ctx.fireChannelRead(((HttpContent) msg).content().retain());
                                ((HttpContent) msg).release();

                                if (msg instanceof LastHttpContent) {
                                    ctx.pipeline()
                                            .remove(this)
                                            .remove(streamReadingHandler);
                                }
                            } else {
                                ctx.fireChannelRead(msg);
                            }
                        }
                    })
                    .remove(this);
            context.read();
        });
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

    public CompletableFuture<ByteBuf> asBuffer() {
        if (decided) {
            throw new IllegalStateException("Already replaced");
        }
        decided = true;

        BufferFutureHandler futureHandler = new BufferFutureHandler();
        context.executor().execute(() -> {
            context.pipeline()
                    .addAfter(context.name(), null, futureHandler)
                    .remove(this);
            context.read();
        });
        return futureHandler.future;
    }
}
