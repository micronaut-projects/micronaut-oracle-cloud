package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.StandardClientProperties;
import com.oracle.bmc.http.client.io.DuplicatableInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@ExtendWith(NettyRule.class)
public class NettyTest {
    private static final HttpProvider PROVIDER = new NettyHttpProvider();

    public NettyRule netty;

    public static void computeContentLength(FullHttpResponse response) {
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    HttpProvider provider() {
        return PROVIDER;
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
            .baseUri(netty.getEndpoint())
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
    public void streamingRequestChunked() throws Exception {
        netty.aggregate = false;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/foo", request.uri());
            Assertions.assertEquals("chunked", request.headers().get("transfer-encoding"));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = provider().newBuilder()
            .baseUri(netty.getEndpoint())
            .property(StandardClientProperties.BUFFER_REQUEST, false)
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
            .baseUri(netty.getEndpoint())
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
            .baseUri(netty.getEndpoint())
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
            .baseUri(netty.getEndpoint())
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
            .baseUri(netty.getEndpoint())
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
    public void connectionReuse() throws Exception {
        Set<Channel> channels = new HashSet<>();
        netty.channelCustomizer = channels::add;
        for (int i = 0; i < 2; i++) {
            netty.handleOneRequest((ctx, request) -> {
                Assertions.assertEquals(HttpMethod.GET, request.method());
                Assertions.assertEquals("/foo", request.uri());

                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
                response.headers().add("Content-Type", "text/plain");
                computeContentLength(response);
                ctx.writeAndFlush(response);
            });
        }

        try (HttpClient client = provider().newBuilder()
            .baseUri(netty.getEndpoint())
            .build()) {
            for (int i = 0; i < 2; i++) {
                try (HttpResponse response = client.createRequest(Method.GET)
                    .appendPathPart("foo")
                    .execute().toCompletableFuture()
                    .get()) {
                    String s = response.textBody().toCompletableFuture().get();
                    Assertions.assertEquals("bar", s);
                }
            }
        }
        // only one connection
        Assertions.assertEquals(1, channels.size());
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

        try (HttpClient lowLevelClient = HttpProvider.getDefault().newBuilder()
            .baseUri(netty.getEndpoint())
            .build()) {
            try (HttpResponse response = lowLevelClient.createRequest(Method.GET).execute().toCompletableFuture().get()) {
                return response.body(type).toCompletableFuture().get();
            }
        }
    }

    public static class MyBean {
        public String s;
    }
}
