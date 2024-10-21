package io.micronaut.oraclecloud.httpclient.apache;

import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.StandardClientProperties;
import com.oracle.bmc.http.client.io.DuplicatableInputStream;
import com.oracle.bmc.streaming.model.PutMessagesDetails;
import com.oracle.bmc.streaming.model.PutMessagesDetailsEntry;
import com.oracle.bmc.streaming.model.PutMessagesResult;
import io.micronaut.serde.annotation.Serdeable;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ExtendWith(NettyRule.class)
public class NettyTest {
    public NettyRule netty;

    public static void computeContentLength(FullHttpResponse response) {
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    HttpProvider provider() {
        return new ApacheHttpProvider();
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

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
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

    @Test
    public void streamingRequestBuffered() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/foo", request.uri());
            Assertions.assertEquals(3, request.headers().getInt("content-length"));
            Assertions.assertEquals("xyz", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void streamingRequest() throws Exception {
        netty.aggregate = false;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/foo", request.uri());
            Assertions.assertEquals("3", request.headers().get("content-length"));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)), 3)
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void streamingRequestBufferedKnownSize() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/foo", request.uri());
            Assertions.assertEquals(3, request.headers().getInt("content-length"));
            Assertions.assertEquals("xyz", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)), 3)
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void onlyUploadIfPositiveResponse() throws Exception {
        netty.handleContinue = true;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals("100-continue", request.headers().get("Expect"));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Unpooled.wrappedBuffer("{\"code\":\"foo\",\"message\":\"bar\"}".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        class FailingInputStream extends InputStream implements DuplicatableInputStream {
            @Override
            public int read() throws IOException {
                throw new AssertionError("Should not be called");
            }

            @Override
            public InputStream duplicate() {
                return this;
            }
        }

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .header("expect", "100-continue")
                .appendPathPart("foo")
                .body(new FailingInputStream())
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(500, response.status());
            }
        }
    }

    @Test
    public void continueBuffer() throws Exception {
        netty.handleContinue = true;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals("100-continue", request.headers().get("Expect"));

            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        });
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals(3, request.headers().getInt("content-length"));
            Assertions.assertEquals("xyz", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .property(StandardClientProperties.BUFFER_REQUEST, true)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .header("expect", "100-continue")
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void bufferByDefault() throws Exception {
        netty.aggregate = false;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals(3, request.headers().getInt("content-length"));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void continueStream() throws Exception {
        netty.handleContinue = true;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals("100-continue", request.headers().get("Expect"));

            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        });
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals(3, request.headers().getInt("content-length"));
            Assertions.assertEquals("xyz", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .header("expect", "100-continue")
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void emptyJsonBody() throws ExecutionException, InterruptedException {
        // we diverge from jax-rs behavior here
        //Assertions.assertArrayEquals(new byte[0], emptyResponseBody(byte[].class));
        Assertions.assertNull(emptyResponseBody(byte[].class));
        //Assertions.assertEquals("", emptyResponseBody(String.class));
        Assertions.assertNull(emptyResponseBody(String.class));

        Assertions.assertNull(emptyResponseBody(Object[].class));
        Assertions.assertNull(emptyResponseBody(MyBean.class));
    }

    private <T> T emptyResponseBody(Class<T> type) throws ExecutionException, InterruptedException {
        netty.handleOneRequest((ctx, request) -> {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.EMPTY_BUFFER
            );
            response.headers().add("Content-Type", "application/json");
            response.headers().add("Content-Length", "0");
            ctx.writeAndFlush(response);
        });

        try (HttpClient lowLevelClient = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .build()) {
            try (HttpResponse response = lowLevelClient.createRequest(Method.GET).execute().toCompletableFuture().get()) {
                return response.body(type).toCompletableFuture().get();
            }
        }
    }

    @Test
    public void streamModelTest() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/", request.uri());
            Assertions.assertEquals("{\"messages\":[{\"key\":\"Zm9v\",\"value\":\"YmFy\"}]}", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer("{\"failures\":1,\"entries\":[]}", StandardCharsets.UTF_8)
            );
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .body(PutMessagesDetails.builder()
                    .messages(List.of(PutMessagesDetailsEntry.builder()
                        .key("foo".getBytes(StandardCharsets.UTF_8))
                        .value("bar".getBytes(StandardCharsets.UTF_8))
                        .build()))
                    .build())
                .execute().toCompletableFuture()
                .get()) {
                PutMessagesResult s = response.body(PutMessagesResult.class).toCompletableFuture().get();
                Assertions.assertEquals(1, s.getFailures());
            }
        }
    }

    @Test
    public void inclusionTest() throws Exception {

        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/", request.uri());
            // empty string should be included in json
            Assertions.assertEquals("{\"s\":\"\"}", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer("foo", StandardCharsets.UTF_8)
            );
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });
        MyBean bean = new MyBean();
        // empty string should be included in json
        bean.s = "";

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .body(bean)
                .execute().toCompletableFuture()
                .get()) {
                String s = response.textBody().toCompletableFuture().get();
                Assertions.assertEquals("foo", s);
            }
        }
    }

    @Test
    @Disabled // TODO: don't really see a way to implement this without threads...
    public void timeoutRetryTest() throws Exception {
        netty.timeout = false; // no server-side timeout
        netty.handleOneRequest((ctx, request) -> {
            // no response sent
        });

        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .build()) {
            try (HttpResponse response = client.createRequest(Method.GET)
                .execute().toCompletableFuture()
                .get()) {
                Assertions.fail();
            } catch (Exception expected) {
                Throwable t = expected;
                while (t != null) {
                    if (t instanceof Exception e && client.isProcessingException(e)) {
                        // condition met
                        return;
                    }
                    t = t.getCause();
                }
                expected.printStackTrace();
                Assertions.fail("Exception is not a processing exception");
            }
        }
    }

    @Test
    public void interceptorOrderTest() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        List<String> intercepts = new ArrayList<>();
        try (HttpClient client = provider().newBuilder()
            .property(ApacheHttpProvider.SOCKET_PATH, netty.getSocketFile())
            .baseUri("https://example.com")
            .registerRequestInterceptor(0, req -> intercepts.add("pr0"))
            .registerRequestInterceptor(2, req -> intercepts.add("pr2"))
            .registerRequestInterceptor(1, req -> intercepts.add("pr1"))
            .build()) {
            client.createRequest(Method.GET)
                .execute().toCompletableFuture()
                .get().close();
        }

        Assertions.assertEquals(List.of("pr0", "pr1", "pr2"), intercepts);
    }

    @Serdeable
    public static class MyBean {
        private String s;

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }
}
