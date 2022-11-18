package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

public interface ExpectedRequestHandler {
    void handle(ChannelHandlerContext ctx, HttpRequest request) throws Exception;
}
