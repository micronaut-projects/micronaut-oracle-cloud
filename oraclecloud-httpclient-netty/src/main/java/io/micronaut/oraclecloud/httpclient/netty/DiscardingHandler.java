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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * Handler that discards incoming data.
 */
@ChannelHandler.Sharable
final class DiscardingHandler extends ChannelInboundHandlerAdapter {
    static final DiscardingHandler INSTANCE = new DiscardingHandler();

    private DiscardingHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ((ByteBuf) msg).release();
            ctx.read();
        } else if (msg instanceof HttpContent) {
            ((HttpContent) msg).release();
            if (msg instanceof LastHttpContent) {
                ctx.pipeline().remove(this);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
