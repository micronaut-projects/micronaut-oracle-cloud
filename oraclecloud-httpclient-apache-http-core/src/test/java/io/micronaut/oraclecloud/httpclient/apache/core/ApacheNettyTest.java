package io.micronaut.oraclecloud.httpclient.apache.core;

import com.oracle.bmc.common.ClientBuilderBase;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import io.micronaut.oraclecloud.httpclient.NettyTest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.nio.NioServerDomainSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;

public class ApacheNettyTest extends NettyTest {
    private Path socketDirectory;
    private Path socketFile;

    HttpProvider provider() {
        return new ApacheCoreHttpProvider();
    }

    @Override
    protected final HttpClientBuilder newBuilder() {
        return provider().newBuilder()
            .baseUri("https://example.com")
            .property(ApacheCoreHttpProvider.SOCKET_PATH, socketFile);
    }

    @Override
    protected final void customize(ClientBuilderBase<?, ?> client) {
        client.httpProvider(provider())
            .additionalClientConfigurator(c -> c.property(ApacheCoreHttpProvider.SOCKET_PATH, socketFile));
    }

    @Override
    protected void setupBootstrap(ServerBootstrap bootstrap) throws Exception {
        socketDirectory = Files.createTempDirectory("oraclecloud-httpclient-apache");
        socketFile = socketDirectory.resolve("socket");
        bootstrap
            .channel(NioServerDomainSocketChannel.class)
            .localAddress(UnixDomainSocketAddress.of(socketFile));
    }

    @AfterEach
    void clean() throws IOException {
        Files.deleteIfExists(socketFile);
        Files.deleteIfExists(socketDirectory);
    }

    @Override
    @Disabled // not implemented
    public void timeoutRetryTest() throws Exception {
        super.timeoutRetryTest();
    }

    @Override
    @Disabled // not implemented
    public void connectionReuse() throws Exception {
        super.connectionReuse();
    }

    @Override
    @Disabled // not implemented
    public void fullSetupTest() throws CertificateException {
        super.fullSetupTest();
    }

    @Test
    public void location() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());
            Assertions.assertEquals("/foo?fizz=buzz", request.uri());
            Assertions.assertEquals("example.com", request.headers().get(HttpHeaderNames.HOST));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder()
            .baseUri("https://example.com")
            .build()) {
            HttpRequest request = client.createRequest(Method.GET)
                .appendPathPart("foo")
                .query("fizz", "buzz");
            Assertions.assertEquals("https://example.com/foo?fizz=buzz", request.uri().toString());
            try (HttpResponse response = request.execute().toCompletableFuture().get()) {
                String s = response.textBody().toCompletableFuture().get();
                Assertions.assertEquals("bar", s);
            }
        }
    }
}
