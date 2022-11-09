package io.micronaut.oraclecloud.httpclient.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.Serialization;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;

@ExtendWith(NettyRule.class)
public class NettyTest {
    private static final ObjectMapper MAPPER = Serialization.getObjectMapper();
    private static final HttpProvider PROVIDER = new NettyHttpProvider();

    public NettyRule netty;

    public static void computeContentLength(FullHttpResponse response) {
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    @Test
    public void simpleRequest() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());
            Assertions.assertEquals("/foo", request.uri());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = PROVIDER.newBuilder()
                .baseUri(netty.getEndpoint())
                .build()) {
            try (HttpResponse response = client.createRequest(Method.GET)
                    .appendPathPart("foo")
                    .execute().toCompletableFuture()
                    .get()) {
                String s = response.textBody().toCompletableFuture().get();
                Assertions.assertEquals("bar", s);
            }
        }
    }
}
