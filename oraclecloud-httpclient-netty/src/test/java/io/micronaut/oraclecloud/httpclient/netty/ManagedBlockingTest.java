package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.internal.ClientCall;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.requests.BmcRequest;
import com.oracle.bmc.responses.BmcResponse;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.waiter.FixedTimeDelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.FastThreadLocalThread;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ManagedBlockingTest {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedBlockingTest.class);

    @Test
    public void testManagedBlocking() throws IOException {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "ManagedBlockingTest",
            "micronaut.netty.event-loops.default.num-threads", 1,
            "micronaut.server.port", 0
        )); EmbeddedServer server = ctx.getBean(EmbeddedServer.class)) {
            server.start();
            try (BlockingHttpClient cl = ctx.createBean(HttpClient.class, server.getURI()).toBlocking()) {
                Assertions.assertEquals("exc", cl.retrieve("/managed-blocking/blocking"));
            }
            BmcException exception = ctx.getBean(MyCtrl.class).exc;
            Throwable cause = exception.getCause();
            Assertions.assertInstanceOf(HttpClientException.class, cause);
            Assertions.assertTrue(cause.getMessage().contains("BlockingHttpClient operation on a netty event loop"));
        }
    }

    @Test
    public void testBlockingFromUnrelatedEventLoop() throws Exception {
        assertBlockingFromUnrelatedEventLoop(false);
    }

    @Test
    public void testBlockingFromUnrelatedEventLoopWithLegacyClient() throws Exception {
        assertBlockingFromUnrelatedEventLoop(true);
    }

    @Test
    public void testPermittedFastThreadLocalThreadIsNotRejected() throws Exception {
        FutureTask<Boolean> task = new FutureTask<>(() -> NettyHttpClient.isBlockingOperationOnEventLoop(Thread.currentThread()));
        FastThreadLocalThread thread = new FastThreadLocalThread(task, "permitted-fast-thread-local") {
            @Override
            public boolean permitBlockingCalls() {
                return true;
            }
        };

        thread.start();

        Assertions.assertFalse(task.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void testBlockingFromRegularThreadSucceeds() throws Exception {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "ManagedBlockingTest",
            "micronaut.server.port", 0
        )); EmbeddedServer server = ctx.getBean(EmbeddedServer.class)) {
            server.start();
            try (com.oracle.bmc.http.client.HttpClient cl = new NettyHttpProvider().newBuilder()
                .baseUri(server.getURI())
                .build()) {
                FakeResponse response = ClientCall.builder(cl, new FakeRequest(), FakeResponse.Builder::new)
                    .logger(LOG, "ManagedBlockingTest")
                    .method(Method.GET)
                    .appendPathPart("/managed-blocking/simple")
                    .callSync();

                Assertions.assertEquals(200, response.get__httpStatusCode__());
            }
        }
    }

    @Test
    public void testExecuteFromEventLoopForRegularBlockingThreadIsNotRejected() throws Exception {
        assertExecuteFromEventLoopForRegularBlockingThread(false);
    }

    @Test
    public void testExecuteFromEventLoopForRegularBlockingThreadIsNotRejectedWithLegacyClient() throws Exception {
        assertExecuteFromEventLoopForRegularBlockingThread(true);
    }

    @Test
    public void testSyncRetryIsNotRejected() throws Exception {
        assertSyncRetry(false);
    }

    @Test
    public void testSyncRetryIsNotRejectedWithLegacyClient() throws Exception {
        assertSyncRetry(true);
    }

    /**
     * The OCI SDK re-executes retried (and token-refreshed) requests from a continuation of the previous
     * attempt, i.e. on the event loop thread that completed it, while the caller of {@code callSync} keeps
     * waiting on its own, non event loop thread. Only the waiting thread must be checked.
     */
    private static void assertExecuteFromEventLoopForRegularBlockingThread(boolean legacyNettyClient) throws Exception {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "ManagedBlockingTest",
            "micronaut.server.port", 0,
            "oci.netty.legacy-netty-client", legacyNettyClient
        )); EmbeddedServer server = ctx.getBean(EmbeddedServer.class)) {
            server.start();
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
            HttpProvider httpProvider = legacyNettyClient ? ctx.getBean(HttpProvider.class) : new NettyHttpProvider();
            try (com.oracle.bmc.http.client.HttpClient cl = httpProvider.newBuilder()
                .baseUri(server.getURI())
                .build()) {
                Assertions.assertEquals(legacyNettyClient, ((NettyHttpClient) cl).legacyNettyClient);
                // offloadExecutor() is what callSync() invokes on the thread that will block
                HttpRequest request = cl.createRequest(Method.GET)
                    .appendPathPart("/managed-blocking/simple")
                    .offloadExecutor(Runnable::run);
                HttpResponse response = eventLoopGroup.next()
                    .submit(request::execute)
                    .get(10, TimeUnit.SECONDS)
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
                try {
                    Assertions.assertEquals(200, response.status());
                } finally {
                    response.close();
                }
            } finally {
                eventLoopGroup.shutdownGracefully().sync();
            }
        }
    }

    private static void assertSyncRetry(boolean legacyNettyClient) throws Exception {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "ManagedBlockingTest",
            "micronaut.server.port", 0,
            "oci.netty.legacy-netty-client", legacyNettyClient
        )); EmbeddedServer server = ctx.getBean(EmbeddedServer.class)) {
            server.start();
            HttpProvider httpProvider = legacyNettyClient ? ctx.getBean(HttpProvider.class) : new NettyHttpProvider();
            try (com.oracle.bmc.http.client.HttpClient cl = httpProvider.newBuilder()
                .baseUri(server.getURI())
                .build()) {
                Assertions.assertEquals(legacyNettyClient, ((NettyHttpClient) cl).legacyNettyClient);
                FakeResponse response = ClientCall.builder(cl, new FakeRequest(), FakeResponse.Builder::new)
                    .logger(LOG, "ManagedBlockingTest")
                    .method(Method.GET)
                    .appendPathPart("/managed-blocking/flaky")
                    .retryConfiguration(RetryConfiguration.builder()
                        .terminationStrategy(new MaxAttemptsTerminationStrategy(3))
                        .delayStrategy(new FixedTimeDelayStrategy(0))
                        .retryCondition(e -> e.getStatusCode() == 503)
                        .build())
                    .callSync();

                Assertions.assertEquals(200, response.get__httpStatusCode__());
                Assertions.assertEquals(2, ctx.getBean(MyCtrl.class).flakyCalls.get());
            }
        }
    }

    private static void assertBlockingFromUnrelatedEventLoop(boolean legacyNettyClient) throws Exception {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "ManagedBlockingTest",
            "micronaut.server.port", 0,
            "oci.netty.legacy-netty-client", legacyNettyClient
        )); EmbeddedServer server = ctx.getBean(EmbeddedServer.class)) {
            server.start();
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
            HttpProvider httpProvider = legacyNettyClient ? ctx.getBean(HttpProvider.class) : new NettyHttpProvider();
            try (com.oracle.bmc.http.client.HttpClient cl = httpProvider.newBuilder()
                .baseUri(server.getURI())
                .build()) {
                Assertions.assertEquals(legacyNettyClient, ((NettyHttpClient) cl).legacyNettyClient);
                BmcException exception = eventLoopGroup.next().submit(() -> {
                    try {
                        ClientCall.builder(cl, new FakeRequest(), FakeResponse.Builder::new)
                            .logger(LOG, "ManagedBlockingTest")
                            .method(Method.GET)
                            .appendPathPart("/managed-blocking/simple")
                            .callSync();
                    } catch (BmcException e) {
                        return e;
                    }
                    return null;
                }).get(10, TimeUnit.SECONDS);

                Assertions.assertNotNull(exception);
                Throwable cause = exception.getCause();
                Assertions.assertInstanceOf(HttpClientException.class, cause);
                Assertions.assertTrue(cause.getMessage().contains("BlockingHttpClient operation on a netty event loop"));
            } finally {
                eventLoopGroup.shutdownGracefully().sync();
            }
        }
    }

    @Requires(property = "spec.name", value = "ManagedBlockingTest")
    @Controller("/managed-blocking")
    static class MyCtrl {
        @Inject
        HttpProvider httpProvider;

        @Inject
        EmbeddedServer me;

        BmcException exc;

        @Get("/blocking")
        //@ExecuteOn(TaskExecutors.BLOCKING)
        String get() {
            try (com.oracle.bmc.http.client.HttpClient cl = httpProvider.newBuilder()
                .baseUri(me.getURI())
                .build()) {
                ClientCall.builder(cl, new FakeRequest(), FakeResponse.Builder::new)
                    .logger(LOG, "ManagedBlockingTest")
                    .method(Method.GET)
                    .appendPathPart("/managed-blocking/simple")
                    .callSync();
            } catch (BmcException e) {
                this.exc = e;
                return "exc";
            }
            return "foo";
        }

        @Get("/simple")
        String simple() {
            return "{}";
        }

        final AtomicInteger flakyCalls = new AtomicInteger();

        @Get("/flaky")
        io.micronaut.http.HttpResponse<String> flaky() {
            if (flakyCalls.incrementAndGet() == 1) {
                return io.micronaut.http.HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("{}");
            }
            return io.micronaut.http.HttpResponse.ok("{}");
        }
    }

    static class FakeRequest extends BmcRequest<FakeRequest> {
    }

    static class FakeResponse extends BmcResponse {
        public FakeResponse(int __httpStatusCode__, Map<String, List<String>> headers) {
            super(__httpStatusCode__, headers);
        }

        static class Builder implements BmcResponse.Builder<FakeResponse> {
            int httpStatusCode;
            Map<String, List<String>> headers;

            @Override
            public BmcResponse.Builder<FakeResponse> __httpStatusCode__(int __httpStatusCode__) {
                this.httpStatusCode = __httpStatusCode__;
                return this;
            }

            @Override
            public BmcResponse.Builder<FakeResponse> headers(Map<String, List<String>> headers) {
                this.headers = headers;
                return this;
            }

            @Override
            public BmcResponse.Builder<FakeResponse> copy(FakeResponse o) {
                this.httpStatusCode = o.get__httpStatusCode__();
                this.headers = o.getHeaders();
                return this;
            }

            @Override
            public FakeResponse build() {
                return new FakeResponse(httpStatusCode, headers);
            }
        }
    }
}
