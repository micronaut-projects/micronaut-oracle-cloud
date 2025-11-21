package io.micronaut.oraclecloud.httpclient;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.common.ClientBuilderBase;
import com.oracle.bmc.encryption.internal.EncryptionHeader;
import com.oracle.bmc.encryption.internal.EncryptionKey;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.Serializer;
import com.oracle.bmc.http.client.StandardClientProperties;
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;
import com.oracle.bmc.http.client.io.DuplicatableInputStream;
import com.oracle.bmc.http.internal.ResponseHelper;
import com.oracle.bmc.io.internal.KeepOpenInputStream;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.CreateAlarmDetails;
import com.oracle.bmc.monitoring.requests.CreateAlarmRequest;
import com.oracle.bmc.monitoring.requests.DeleteAlarmRequest;
import com.oracle.bmc.streaming.model.PutMessagesDetails;
import com.oracle.bmc.streaming.model.PutMessagesDetailsEntry;
import com.oracle.bmc.streaming.model.PutMessagesResult;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.serde.annotation.SerdeImport;
import io.micronaut.serde.annotation.Serdeable;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(NettyRule.class)
@SerdeImport(CreateAlarmDetails.class)
public abstract class NettyTest {
    protected NettyRule netty;

    public static void computeContentLength(FullHttpResponse response) {
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }

    protected abstract Serializer serializer();

    protected abstract HttpClientBuilder newBuilder();

    protected abstract String endpoint();

    protected abstract void customize(ClientBuilderBase<?, ?> client);

    protected abstract void setupBootstrap(ServerBootstrap bootstrap) throws Exception;

    protected final Channel getServerChannel() {
        return netty.serverChannel;
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

        try (HttpClient client = newBuilder()
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
    public void textBodyTwice() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());
            Assertions.assertEquals("/foo", request.uri());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder()
            .build()) {
            try (HttpResponse response = client.createRequest(Method.GET)
                .appendPathPart("foo")
                .execute().toCompletableFuture()
                .get()) {

                Assertions.assertEquals("bar", response.textBody().toCompletableFuture().get());
                Assertions.assertEquals("bar", response.textBody().toCompletableFuture().get());
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

        try (HttpClient client = newBuilder()
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .header("content-type", "text/plain")
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

        try (HttpClient client = newBuilder()
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)), 3)
                .header("content-type", "text/plain")
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

        try (HttpClient client = newBuilder()
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)), 3)
                .header("content-type", "text/plain")
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    @Timeout(10)
    public void streamingResponse() throws Exception {
        int chunkSize = 8192;
        int numberOfChunks = 10;

        netty.aggregate = false;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());
            Assertions.assertEquals("/foo", request.uri());

            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().add("content-length", chunkSize * numberOfChunks);
            ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                int n = numberOfChunks;

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (n-- == 0) {
                        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, ctx.voidPromise());
                    } else {
                        ctx.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(new byte[chunkSize]))).addListener(this);
                    }
                }
            });
        });

        try (
            HttpClient client = newBuilder()
                .property(StandardClientProperties.BUFFER_REQUEST, false)
                .build();
            HttpResponse response = client.createRequest(Method.GET)
                .appendPathPart("foo")
                .execute().toCompletableFuture()
                .get();
            InputStream stream = response.streamBody().toCompletableFuture().get()) {

            Assertions.assertEquals(200, response.status());
            Assertions.assertEquals(chunkSize * numberOfChunks, stream.readAllBytes().length);
        }
    }

    @Test
    public void onlyUploadIfPositiveResponse() throws Exception {
        netty.handleContinue = true;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals("100-continue", request.headers().get("Expect"));
            Assertions.assertEquals("3", request.headers().get("content-length"));

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

        try (HttpClient client = newBuilder()
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .header("expect", "100-continue")
                .appendPathPart("foo")
                .body(new FailingInputStream(), 3)
                .header("content-type", "text/plain")
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

        try (HttpClient client = newBuilder()
            .property(StandardClientProperties.BUFFER_REQUEST, true)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .header("expect", "100-continue")
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .header("content-type", "text/plain")
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void bufferWithoutLength() throws Exception {
        // even when BUFFER_REQUEST is set to false, the jersey client will buffer requests that
        // are not given with explicit length, and those requests will then get a content-length.

        netty.aggregate = false;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals(3, request.headers().getInt("content-length"));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder()
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .header("content-type", "text/plain")
                .execute().toCompletableFuture()
                .get()) {
                Assertions.assertEquals(200, response.status());
            }
        }
    }

    @Test
    public void bufferWithLength() throws Exception {
        netty.aggregate = false;
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(request.method(), HttpMethod.PUT);
            Assertions.assertEquals(3, request.headers().getInt("content-length"));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder()
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)), 3)
                .header("content-type", "text/plain")
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

        try (HttpClient client = newBuilder()
            .property(StandardClientProperties.BUFFER_REQUEST, false)
            .build()) {
            try (HttpResponse response = client.createRequest(Method.PUT)
                .header("expect", "100-continue")
                .appendPathPart("foo")
                .body(new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)))
                .header("content-type", "text/plain")
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

        try (HttpClient client = newBuilder()
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

        try (HttpClient lowLevelClient = newBuilder()
            .build()) {
            try (HttpResponse response = lowLevelClient.createRequest(Method.GET).execute().toCompletableFuture().get()) {
                return response.body(type).toCompletableFuture().get();
            }
        }
    }

    private SimpleAuthenticationDetailsProvider mockAuthenticationDetailsProvider() throws CertificateException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SimpleAuthenticationDetailsProvider.builder()
            .tenantId("tenantId")
            .userId("userId")
            .fingerprint("fingerprint")
            .passPhrase("")
            .region(Region.US_PHOENIX_1)
            .privateKeySupplier(() -> {
                try {
                    return new FileInputStream(ssc.privateKey());
                } catch (FileNotFoundException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .build();
    }

    @Test
    public void fullSetupTest() throws CertificateException {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.DELETE, request.method());
            Assertions.assertEquals("/20180401/alarms/foo", request.uri());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer("{}", StandardCharsets.UTF_8)
            );
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        MonitoringClient.Builder builder = MonitoringClient.builder();
        customize(builder);
        try (MonitoringClient monitoringClient = builder
            .build(mockAuthenticationDetailsProvider())) {

            monitoringClient.deleteAlarm(DeleteAlarmRequest.builder()
                .alarmId("foo")
                .build());
        }
    }

    @Test
    public void fullSetupConnectionDiesMidError() throws CertificateException {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/20180401/alarms", request.uri());

            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            response.headers().add("Content-Type", "application/json");
            response.headers().add("Content-Length", "2");
            ctx.write(response, ctx.voidPromise());
            ctx.writeAndFlush(new DefaultHttpContent(Unpooled.copiedBuffer("{", StandardCharsets.UTF_8)))
                .addListener(ChannelFutureListener.CLOSE);
        });

        MonitoringClient.Builder builder = MonitoringClient.builder();
        customize(builder);
        try (MonitoringClient monitoringClient = builder
            .build(mockAuthenticationDetailsProvider())) {

            BmcException exc = assertThrows(BmcException.class, () -> monitoringClient.createAlarm(CreateAlarmRequest.builder()
                .createAlarmDetails(CreateAlarmDetails.builder().build())
                .build()));
            try {
                assertTrue(exc.getMessage().contains("Unable to parse error response."));
            } catch (Throwable e) {
                e.addSuppressed(exc);
                throw e;
            }
        }
    }

    @Test
    public void fullSetupErrorParseFailure() throws CertificateException {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/20180401/alarms", request.uri());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST,
                Unpooled.copiedBuffer("{", StandardCharsets.UTF_8)
            );
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        MonitoringClient.Builder builder = MonitoringClient.builder();
        customize(builder);
        try (MonitoringClient monitoringClient = builder
            .build(mockAuthenticationDetailsProvider())) {

            BmcException exc = assertThrows(BmcException.class, () -> monitoringClient.createAlarm(CreateAlarmRequest.builder()
                .createAlarmDetails(CreateAlarmDetails.builder().build())
                .build()));
            try {
                assertTrue(exc.getMessage().contains("Unable to parse error response: {"));
            } catch (Throwable e) {
                e.addSuppressed(exc);
                throw e;
            }
        }
    }

    @Test
    public void fullSetupTestManagedCustomProperties() throws CertificateException {
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            netty.handleOneRequest((ctx, request) -> {
                Assertions.assertEquals(HttpMethod.DELETE, request.method());
                Assertions.assertEquals("/20180401/alarms/foo", request.uri());
                DefaultFullHttpResponse response;

                if (finalI < 2) {
                    response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        Unpooled.copiedBuffer("{}", StandardCharsets.UTF_8)

                    );
                } else {
                    response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        Unpooled.copiedBuffer("{}", StandardCharsets.UTF_8)
                    );
                }
                response.headers().add("Content-Type", "application/json");
                computeContentLength(response);
                ctx.writeAndFlush(response);
            });
        }

        try (ApplicationContext ctx = ApplicationContext.run(
            Map.of("oci.clients.monitoring.retry-termination-strategy.max-attempts", "3",
                "oci.clients.monitoring.retry-delay-strategy.time-between-attempts-in-millis", "100"),
            Environment.ORACLE_CLOUD);
        ) {
            MonitoringClient.Builder builder = ctx.getBean(MonitoringClient.Builder.class);
            customize(builder);
            try (MonitoringClient monitoringClient = builder
                .build(mockAuthenticationDetailsProvider())) {

                monitoringClient.deleteAlarm(DeleteAlarmRequest.builder()
                    .alarmId("foo")
                    .build());
            }
        }
    }

    @Test
    public void functionsClientTest() throws CertificateException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        byte[] body = new byte[10];
        ThreadLocalRandom.current().nextBytes(body);

        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/20181201/functions/function-id/actions/invoke", request.uri());
            Assertions.assertTrue(request.headers().get(HttpHeaderNames.AUTHORIZATION).contains("content-length"));

            SignatureV1.verify((FullHttpRequest) request, ssc.cert().getPublicKey());

            Assertions.assertArrayEquals(body, ByteBufUtil.getBytes(((FullHttpRequest) request).content()));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer("{}", StandardCharsets.UTF_8)
            );
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        FunctionsInvokeClient.Builder builder = FunctionsInvokeClient.builder();
        customize(builder);
        try (FunctionsInvokeClient invokeClient = builder
            .build(SimpleAuthenticationDetailsProvider.builder()
                .tenantId("tenantId")
                .userId("userId")
                .fingerprint("fingerprint")
                .passPhrase("")
                .region(Region.US_PHOENIX_1)
                .privateKeySupplier(() -> {
                    try {
                        return new FileInputStream(ssc.privateKey());
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .build())) {

            invokeClient.invokeFunction(InvokeFunctionRequest.builder()
                .functionId("function-id")
                .fnIntent(InvokeFunctionRequest.FnIntent.Httprequest)
                .fnInvokeType(InvokeFunctionRequest.FnInvokeType.Sync)
                .invokeFunctionBody(new KeepOpenInputStream(new ByteArrayInputStream(body)))
                .build());
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

        try (HttpClient client = newBuilder()
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .body(PutMessagesDetails.builder()
                    .messages(List.of(PutMessagesDetailsEntry.builder()
                        .key("foo".getBytes(StandardCharsets.UTF_8))
                        .value("bar".getBytes(StandardCharsets.UTF_8))
                        .build()))
                    .build())
                .header("content-type", "application/json")
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

        try (HttpClient client = newBuilder()
            .build()) {
            try (HttpResponse response = client.createRequest(Method.POST)
                .body(bean)
                .header("content-type", "application/json")
                .execute().toCompletableFuture()
                .get()) {
                String s = response.textBody().toCompletableFuture().get();
                Assertions.assertEquals("foo", s);
            }
        }
    }

    @Test
    @Timeout(60)
    public void timeoutRetryTest() throws Exception {
        netty.timeout = false; // no server-side timeout
        netty.handleOneRequest((ctx, request) -> {
            // no response sent
        });

        try (HttpClient client = newBuilder()
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
        try (HttpClient client = newBuilder()
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

    @Test
    public void explicitlySetTest() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/", request.uri());
            Assertions.assertEquals("{\"foo\":null}", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        ExplicitlySetBean bean = new ExplicitlySetBean(null, null);
        bean.markPropertyAsExplicitlySet("foo");

        try (HttpClient client = newBuilder().build()) {
            client.createRequest(Method.POST)
                .body(bean)
                .header("content-type", "application/json")
                .execute().toCompletableFuture()
                .get().close();
        }
    }

    @Test
    public void dateFormatTest() throws Exception {
        // the original java-sdk forces serializing the millis here, even when they are 0. We don't
        // support that behavior in serde, so this only tests non-0 millis :)
        String instant = "2024-10-23T18:47:01.001Z";
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/", request.uri());
            Assertions.assertEquals("{\"date\":\"" + instant + "\"}", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder().build()) {
            client.createRequest(Method.POST)
                .body(new DateBean(Date.from(Instant.parse(instant))))
                .header("content-type", "application/json")
                .execute().toCompletableFuture()
                .get().close();
        }
    }

    @Test
    public void errorCodeAndMessageTest() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer("{\"code\":\"MyCode\",\"message\":\"msg\"}", StandardCharsets.UTF_8));
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder().build();
             HttpResponse response = client.createRequest(Method.GET)
                 .execute().toCompletableFuture()
                 .get()) {
            ResponseHelper.ErrorCodeAndMessage resp = response.body(ResponseHelper.ErrorCodeAndMessage.class).toCompletableFuture().get();
            Assertions.assertEquals("MyCode", resp.getCode());
            Assertions.assertEquals("msg", resp.getMessage());
        }
    }

    @Test
    public void sessionTokenTest() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.POST, request.method());
            Assertions.assertEquals("/", request.uri());
            Assertions.assertEquals("{\"currentToken\":\"xyz\"}", ((FullHttpRequest) request).content().toString(StandardCharsets.UTF_8));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer("{\"token\":\"foo\"}", StandardCharsets.UTF_8));
            response.headers().add("Content-Type", "application/json");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder().build();
             HttpResponse response = client.createRequest(Method.POST)
                 .body(new SessionTokenAuthenticationDetailsProvider.SessionTokenRefreshRequest.SessionTokenRequest("xyz"))
                 .header("content-type", "application/json")
                 .execute().toCompletableFuture()
                 .get()) {
            SessionTokenAuthenticationDetailsProvider.SessionToken resp = response.body(SessionTokenAuthenticationDetailsProvider.SessionToken.class).toCompletableFuture().get();
            Assertions.assertEquals("foo", resp.getToken());
        }
    }

    @Test
    public void pipeTest() throws Exception {
        String testString = "foo|bar";
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());
            Assertions.assertEquals("/" + URLEncoder.encode(testString, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT), request.uri().toLowerCase(Locale.ROOT));

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder()
            .build()) {
            HttpRequest request = client.createRequest(Method.GET)
                .appendPathPart(testString);
            Assertions.assertEquals("/" + testString, request.uri().getPath());
            try (HttpResponse response = request
                .execute().toCompletableFuture()
                .get()) {
                String s = response.textBody().toCompletableFuture().get();
                Assertions.assertEquals("bar", s);
            }
        }
    }

    @Test
    public void localBaseUriTest() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder()
            .baseUri(endpoint())
            .build()) {

            HttpRequest request = client.createRequest(Method.GET);
            try (HttpResponse response = request.execute().toCompletableFuture().get()) {
                String s = response.textBody().toCompletableFuture().get();
                Assertions.assertEquals("bar", s);
            }
        }
    }
    @Disabled
    @Test
    public void localBaseUriTestInvalid() throws Exception {
        netty.handleOneRequest((ctx, request) -> {
            Assertions.assertEquals(HttpMethod.GET, request.method());

            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
            response.headers().add("Content-Type", "text/plain");
            computeContentLength(response);
            ctx.writeAndFlush(response);
        });

        try (HttpClient client = newBuilder()
            .baseUri("https://example.com[]") // invalid
            .build()) {
            client.updateEndpoint(endpoint());

            HttpRequest request = client.createRequest(Method.GET);
            try (HttpResponse response = request.execute().toCompletableFuture().get()) {
                String s = response.textBody().toCompletableFuture().get();
                Assertions.assertEquals("bar", s);
            }
        }
    }

    @Test
    public void encryptionSdk() throws IOException {
        EncryptionHeader h = new EncryptionHeader();
        h.setEncryptionHeader(
            new EncryptionKey("region", "vaultId", "masterKeyId", "encryptedDataKey"),
            "iv",
            "additionalAuthenticatedData"
        );
        String json = serializer().writeValueAsString(h);

        EncryptionHeader deserialized = serializer().readValue(json, EncryptionHeader.class);
        Assertions.assertEquals(h.getEncryptionKey(), deserialized.getEncryptionKey());
        Assertions.assertEquals(h.getIV(), deserialized.getIV());
        Assertions.assertEquals(h.getAdditionalAuthenticatedData(), deserialized.getAdditionalAuthenticatedData());
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

    @Serdeable
    public static class ExplicitlySetBean extends ExplicitlySetBmcModel {
        private final String foo;
        private final String bar;

        public ExplicitlySetBean(String foo, String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        @Override
        public void markPropertyAsExplicitlySet(String propertyName) {
            super.markPropertyAsExplicitlySet(propertyName);
        }

        public String getFoo() {
            return foo;
        }

        public String getBar() {
            return bar;
        }
    }

    @Serdeable
    public static class DateBean {
        private final Date date;

        public DateBean(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }
    }
}
