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
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import java.util.concurrent.CompletableFuture;

/**
 * This channel handler accumulates input data ({@link ByteBuf} and {@link HttpContent}), and when
 * {@link LastHttpContent} is received completes a {@link #future} with the accumulated data.
 */
final class BufferFutureHandler extends ChannelInboundHandlerAdapter {
    final CompletableFuture<ByteBuf> future = new CompletableFuture<>();
    private CompositeByteBuf buffer;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        buffer = ctx.alloc().compositeBuffer();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (buffer != null) {
            future.cancel(false);
            buffer.release();
            buffer = null;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpContent) {
            channelRead(ctx, ((HttpContent) msg).content().retain());
            ((HttpContent) msg).release();

            if (msg instanceof LastHttpContent) {
                if (!future.complete(buffer.retain())) {
                    buffer.release();
                }
                buffer.release();
                buffer = null;
                ctx.pipeline().remove(this);
            } else {
                ctx.read();
            }
        } else if (msg instanceof ByteBuf) {
            buffer.addComponent(true, (ByteBuf) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!future.completeExceptionally(cause)) {
            ctx.fireExceptionCaught(cause);
        }
    }
}
