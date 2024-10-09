package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.StandardClientProperties;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

class NettyHttpClientTest {
    @Test
    public void simple() throws Exception {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("micronaut.server.port", "-1");
        ApplicationContext ctx = ApplicationContext.run(properties);
        EmbeddedServer embeddedServer = ctx.getBean(EmbeddedServer.class);
        embeddedServer.start();

        try (HttpClient client = new NettyHttpClientBuilder(null)
                .baseUri(embeddedServer.getURI())
                .build()) {
            Map body = client.createRequest(Method.GET)
                    .appendPathPart("/foo")
                    .execute().toCompletableFuture().get()
                    .body(Map.class).toCompletableFuture().get();
            Map<String, String> expected = new java.util.HashMap<>();
            expected.put("foo", "bar");
            Assertions.assertEquals(expected, body);
        }
    }

    @Test
    public void echoStreams() throws Exception {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("micronaut.server.port", "8080");
        ApplicationContext ctx = ApplicationContext.run(properties);
        EmbeddedServer embeddedServer = ctx.getBean(EmbeddedServer.class);
        embeddedServer.start();

        try (HttpClient client = new NettyHttpClientBuilder(null)
                .baseUri(embeddedServer.getURI())
                .build()) {
            InputStream responseStream = client.createRequest(Method.POST)
                    .appendPathPart("/echoChunked")
                    .body(new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)))
                    .execute().toCompletableFuture().get()
                    .streamBody().toCompletableFuture().get();
            Assertions.assertEquals("abcdef", new String(toByteArray(responseStream, 6), StandardCharsets.UTF_8));
        }
    }

    private static byte[] toByteArray(InputStream stream, int n) throws IOException {
        byte[] b = new byte[n];
        for (int i = 0; i < n;) {
            int k = stream.read(b, i, n - i);
            if (k == -1) {
                throw new EOFException();
            }
            i += k;
        }
        return b;
    }

    @Test
    public void echoText() throws Exception {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("micronaut.server.port", "-1");
        ApplicationContext ctx = ApplicationContext.run(properties);
        EmbeddedServer embeddedServer = ctx.getBean(EmbeddedServer.class);
        embeddedServer.start();

        try (HttpClient client = new NettyHttpClientBuilder(null)
                .baseUri(embeddedServer.getURI())
                .build()) {
            String response = client.createRequest(Method.POST)
                    .appendPathPart("/echo")
                    .body("abcdef")
                    .execute().toCompletableFuture().get()
                    .textBody().toCompletableFuture().get();
            Assertions.assertEquals("abcdef", response);
        }
    }

    @Test
    public void readTimeoutText() throws Exception {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("micronaut.server.port", "-1");
        ApplicationContext ctx = ApplicationContext.run(properties);
        EmbeddedServer embeddedServer = ctx.getBean(EmbeddedServer.class);
        embeddedServer.start();

        // With read timeout 1 millis, call endpoint working 2 or more millis
        // should fail
        try (HttpClient client = new NettyHttpClientBuilder(null)
            .property(StandardClientProperties.READ_TIMEOUT, Duration.ofMillis(1))
            .baseUri(embeddedServer.getURI())
            .build()) {
            CompletionStage<HttpResponse> result = client.createRequest(Method.GET)
                .appendPathPart("/slow")
                .execute()
                .exceptionally(e -> {
                    if (e instanceof CompletionException) {
                        e = e.getCause();
                    }
                    Assertions.assertInstanceOf(ReadTimeoutException.class, e);
                    return null;
                });
            Assertions.assertNull(result.toCompletableFuture().get());
        }
    }

    @Controller
    public static class Ctrl {
        @Get("/foo")
        public String getFoo() {
            return "{\"foo\":\"bar\"}";
        }

        @Post("/echo")
        public String echo(@Body String body) {
            return body;
        }

        @Post("/echoChunked")
        public Publisher<byte[]> echoChunked(@Body Publisher<byte[]> body) {
            return body;
        }

        @Get("/slow")
        public String getSlow() throws Exception {
            // For test with read timeout of 1ms
            Thread.sleep(2);
            return "slow_response";
        }
    }
}
