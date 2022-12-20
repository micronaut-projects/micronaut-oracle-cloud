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
 * Handler that buffers some response bytes until a set limit. This way, when normal body reading fails, we can still
 * read a potentially short error message from this handler.
 */
final class LimitedBufferingBodyHandler extends ChannelInboundHandlerAdapter {
    private final int maxBuffer;
    private CompositeByteBuf buffer;
    private boolean overflowed = false;
    private final CompletableFuture<ByteBuf> future = new CompletableFuture<>();

    LimitedBufferingBodyHandler(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        buffer = ctx.alloc().compositeBuffer();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (buffer != null) {
            buffer.release();
            buffer = null;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpContent && !overflowed) {
            ByteBuf content = ((HttpContent) msg).content();
            int toAdd = Math.min(maxBuffer - buffer.readableBytes(), content.readableBytes());
            if (toAdd > 0) {
                overflowed = toAdd < content.readableBytes();
                ByteBuf slice = content.retainedSlice(buffer.readerIndex(), toAdd);
                buffer.addComponent(true, slice);
            }
            if (msg instanceof LastHttpContent || buffer.readableBytes() >= maxBuffer) {
                future.complete(buffer);
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        future.completeExceptionally(cause);
        super.exceptionCaught(ctx, cause);
    }

    /**
     * Future for the buffer. Note: Because this buffer isn't always used, it is *not* retained for the receiver of
     * this future! On completion, the buffer must be retained immediately.
     *
     * @return Future that contains the buffered data
     */
    public CompletableFuture<ByteBuf> getFuture() {
        return future;
    }

    public boolean hasOverflowed() {
        return overflowed;
    }
}
