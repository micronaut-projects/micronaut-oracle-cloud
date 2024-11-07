package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.common.ClientBuilderBase;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import io.micronaut.oraclecloud.httpclient.NettyTest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.micronaut.oraclecloud.httpclient.netty.NettyClientProperties.OCI_NETTY_CLIENT_FILTERS_KEY;

public class NettyUnmanagedTest extends NettyTest {
    private static final HttpProvider PROVIDER = new NettyHttpProvider();

    HttpProvider provider() {
        return PROVIDER;
    }

    @Override
    protected HttpClientBuilder newBuilder() {
        return provider().newBuilder().baseUri(getEndpoint());
    }

    @Override
    protected void customize(ClientBuilderBase<?, ?> client) {
        client.httpProvider(provider())
            .endpoint(getEndpoint());
    }

    @Override
    protected void setupBootstrap(ServerBootstrap bootstrap) {
        bootstrap
            .channel(NioServerSocketChannel.class)
            .localAddress("127.0.0.1", 0);
    }

    private String getEndpoint() {
        InetSocketAddress addr = (InetSocketAddress) getServerChannel().localAddress();
        return "http://" + addr.getHostString() + ":" + addr.getPort();
    }

    @Test
    void simpleRequestTestFilters() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());
            Assertions.assertEquals("/foo", request.uri());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        NettyHttpClientBuilder clientBuilder = (NettyHttpClientBuilder) newBuilder();

        FirstTestNettyClientFilter firstTestNettyClientFilter = new FirstTestNettyClientFilter();
        SecondTestNettyClientFilter secondTestNettyClientFilter = new SecondTestNettyClientFilter();

        clientBuilder.property(OCI_NETTY_CLIENT_FILTERS_KEY, List.of(
            firstTestNettyClientFilter,
            secondTestNettyClientFilter
        ));

        HttpClient client = clientBuilder.build();
        try (HttpResponse response = client.createRequest(Method.GET)
            .appendPathPart("foo")
            .execute().toCompletableFuture()
            .get()) {
            String s = response.textBody().toCompletableFuture().get();
            Assertions.assertEquals("bar", s);
        }
        client.close();

        Assertions.assertNotEquals(0, firstTestNettyClientFilter.getStartTime());
        Assertions.assertNotEquals(0, firstTestNettyClientFilter.getEndTime());
        Assertions.assertNotEquals(0, secondTestNettyClientFilter.getStartTime());
        Assertions.assertNotEquals(0, secondTestNettyClientFilter.getEndTime());

        Assertions.assertTrue(firstTestNettyClientFilter.getStartTime() < firstTestNettyClientFilter.getEndTime());
        Assertions.assertTrue(secondTestNettyClientFilter.getStartTime() < secondTestNettyClientFilter.getEndTime());
        Assertions.assertTrue(firstTestNettyClientFilter.getStartTime() < secondTestNettyClientFilter.getEndTime());
        Assertions.assertTrue(secondTestNettyClientFilter.getStartTime() < firstTestNettyClientFilter.getEndTime());

        Assertions.assertTrue(firstTestNettyClientFilter.getStartTime() < secondTestNettyClientFilter.getStartTime());
        Assertions.assertTrue(firstTestNettyClientFilter.getOrder() < secondTestNettyClientFilter.getOrder());
        Assertions.assertTrue(firstTestNettyClientFilter.getEndTime() < secondTestNettyClientFilter.getEndTime());
    }
}
