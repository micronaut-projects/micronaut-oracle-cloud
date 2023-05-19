/*
 * Copyright 2017-2023 original authors
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
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Base class for handlers that replace the {@link UndecidedBodyHandler} once the user has decided how they want to
 * consume the body. First any data buffered by {@link UndecidedBodyHandler} is processed, then a {@link HandlerImpl}
 * is added to the pipeline to directly process the remaining chunks.
 */
abstract class DecidedBodyHandler {
    private boolean done = false;
    private volatile ChannelHandlerContext context;
    private final List<Runnable> pendingContextActions = new ArrayList<>();

    private void runWithContext(Runnable r) {
        if (context != null) {
            r.run();
            return;
        }
        synchronized (pendingContextActions) {
            if (context != null) {
                r.run();
                return;
            }
            pendingContextActions.add(r);
        }
    }

    /**
     * Trigger an upstream {@link ChannelHandlerContext#read()}.
     */
    final void triggerUpstreamRead() {
        runWithContext(() -> context.read());
    }

    /**
     * Best-effort check that this is not called in the event loop.
     */
    final void checkNotOnEventLoop() {
        // embedded channel always returns true for inEventLoop
        if (context != null && context.executor().inEventLoop() && !(context.channel() instanceof EmbeddedChannel)) {
            throw new IllegalStateException("This method must not be called on the netty event loop");
        }
    }

    /**
     * Signal early cancellation by the user, remove this handler.
     */
    final void removeEarly() {
        runWithContext(() -> {
            try {
                context.pipeline().remove(context.handler());
            } catch (NoSuchElementException ignored) {
            }
        });
    }

    /**
     * Handle an error in the pipeline.
     *
     * @param cause The error
     * @return {@code true} if we were able to forward the error to the user, {@code false} if it could not be handled
     * and should be forwarded through the pipeline.
     */
    abstract boolean onError(Throwable cause);

    /**
     * Handle incoming data.
     *
     * @param data The incoming data buffer.
     */
    abstract void onData(ByteBuf data);

    /**
     * Handle a {@link LastHttpContent} signalling that all data has been received successfully.
     */
    abstract void onComplete();

    /**
     * Handle an early cancellation (e.g. channel close).
     */
    final void onCancel() {
        if (!done) {
            onError(new EOFException());
        }
    }

    final void onContent(HttpContent msg) {
        onData(msg.content().retain());
        msg.release();

        if (msg instanceof LastHttpContent) {
            done = true;
            onComplete();
        }
    }

    /**
     * {@link io.netty.channel.ChannelHandler} that forwards to this {@link DecidedBodyHandler}.
     */
    final class HandlerImpl extends ChannelInboundHandlerAdapter {
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            context = ctx;
            synchronized (pendingContextActions) {
                for (Runnable action : pendingContextActions) {
                    action.run();
                }
                pendingContextActions.clear();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (!onError(cause)) {
                ctx.fireExceptionCaught(cause);
            }
            ctx.pipeline().remove(this);
            done = true;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpContent) {
                onContent((HttpContent) msg);
                if (done) {
                    ctx.pipeline().remove(this);
                } else {
                    ctx.read();
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            onCancel();
        }
    }
}
