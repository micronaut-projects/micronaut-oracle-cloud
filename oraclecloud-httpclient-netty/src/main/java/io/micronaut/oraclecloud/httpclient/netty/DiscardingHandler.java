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
