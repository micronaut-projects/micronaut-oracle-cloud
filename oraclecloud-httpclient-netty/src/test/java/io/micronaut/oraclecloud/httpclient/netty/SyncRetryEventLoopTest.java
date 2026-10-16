package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.internal.ClientCall;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(30)
class SyncRetryEventLoopTest {
    private static final Logger LOG = LoggerFactory.getLogger(SyncRetryEventLoopTest.class);

    @Test
    void sharedEventLoopRemainsResponsiveDuringSyncRetry() throws Exception {
        checkRetry(false, false);
    }

    @Test
    void dedicatedFiveThreadPoolDoesNotRunSyncRetryDelay() throws Exception {
        checkRetry(true, false);
    }

    @Test
    void legacySharedEventLoopRemainsResponsiveDuringSyncRetry() throws Exception {
        checkRetry(false, true);
    }

    @Test
    void legacyDedicatedFiveThreadPoolDoesNotRunSyncRetryDelay() throws Exception {
        checkRetry(true, true);
    }

    private void checkRetry(boolean isolated, boolean legacy) throws Exception {
        Map<String, Object> properties = new HashMap<>(Map.of(
            "spec.name", "SyncRetryEventLoopTest",
            "micronaut.server.port", -1,
            "micronaut.server.thread-selection", "BLOCKING",
            "micronaut.netty.event-loops.default.num-threads", 1,
            "micronaut.http.services.oci.read-timeout", "200ms",
            "endpoints.health.sensitive", false,
            "oci.netty.legacy-netty-client", legacy
        ));
        if (isolated) {
            properties.put("micronaut.netty.event-loops.oci-pool.num-threads", 5);
            properties.put("micronaut.http.services.oci.event-loop-group", "oci-pool");
        }

        AtomicReference<Thread> callerThread = new AtomicReference<>();
        AtomicReference<Thread> retryThread = new AtomicReference<>();
        CountDownLatch backoff = new CountDownLatch(1);
        RetryConfiguration retryConfiguration = RetryConfiguration.builder()
            .terminationStrategy(new MaxAttemptsTerminationStrategy(2))
            .delayStrategy(context -> {
                retryThread.set(Thread.currentThread());
                backoff.countDown();
                return 2000;
            })
            .build();

        var caller = Executors.newSingleThreadExecutor();
        try (ApplicationContext context = ApplicationContext.run(properties);
             EmbeddedServer server = context.getBean(EmbeddedServer.class)) {
            server.start();
            URI health = server.getURI().resolve("/health");
            assertEquals(200, healthStatus(health));

            try (var client = context.getBean(HttpProvider.class).newBuilder().baseUri(server.getURI()).build()) {
                CompletableFuture<BmcException> request = CompletableFuture.supplyAsync(() -> {
                    callerThread.set(Thread.currentThread());
                    try {
                        ClientCall.builder(client, new ManagedBlockingTest.FakeRequest(), ManagedBlockingTest.FakeResponse.Builder::new)
                            .logger(LOG, "syncRetryEventLoop")
                            .method(Method.DELETE)
                            .appendPathPart("/retry-timeout")
                            .operationUsesDefaultRetries()
                            .retryConfiguration(retryConfiguration)
                            .callSync();
                        throw new AssertionError("Expected the upstream request to time out");
                    } catch (BmcException e) {
                        return e;
                    }
                }, caller);

                assertTrue(backoff.await(10, TimeUnit.SECONDS), "The timeout did not trigger a retry");
                String stack = Arrays.toString(retryThread.get().getStackTrace());
                long started = System.nanoTime();
                Exception probeFailure = null;
                int status = 0;
                try {
                    status = healthStatus(health);
                } catch (Exception e) {
                    probeFailure = e;
                }
                long healthMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                Throwable failure = request.get(10, TimeUnit.SECONDS);
                while (failure.getCause() != null) {
                    failure = failure.getCause();
                }
                if (legacy) {
                    assertInstanceOf(io.netty.handler.timeout.ReadTimeoutException.class, failure);
                } else {
                    assertInstanceOf(ReadTimeoutException.class, failure);
                }
                assertEquals(2, context.getBean(Upstream.class).requests.get());

                System.out.printf("isolated=%s legacy=%s caller=%s retry=%s health=%s elapsed=%dms%n",
                    isolated, legacy, callerThread.get().getName(), retryThread.get().getName(),
                    probeFailure == null ? status : probeFailure, healthMillis);
                System.out.println("Retry thread stack: " + stack);
                assertSame(callerThread.get(), retryThread.get(), "The synchronous retry delay ran on the HTTP event loop");
                assertEquals(200, status, "Health request failed during retry: " + probeFailure);
            }
        } finally {
            caller.shutdownNow();
        }
    }

    private static int healthStatus(URI health) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) health.toURL().openConnection();
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    @Controller
    @Requires(property = "spec.name", value = "SyncRetryEventLoopTest")
    static class Upstream {
        final AtomicInteger requests = new AtomicInteger();

        @Delete("/retry-timeout")
        CompletableFuture<String> timeout() {
            requests.incrementAndGet();
            return new CompletableFuture<>();
        }
    }
}
