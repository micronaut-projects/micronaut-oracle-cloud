package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.StandardClientProperties;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LargeTransferTest {
    @Test
    public void test() throws Exception {
        long count = 1_000_000_000;
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "LargeTransferTest",
            "micronaut.server.ssl.enabled", true,
            "micronaut.server.ssl.port", -1,
            "micronaut.server.ssl.build-self-signed", true,
            "micronaut.server.max-request-size", count * 2,
            "micronaut.http.client.ssl.insecure-trust-all-certificates", true
        ));
             EmbeddedServer server = ctx.getBean(EmbeddedServer.class)) {
            server.start();

            try (HttpClient client = ctx.getBean(HttpProvider.class).newBuilder()
                .property(StandardClientProperties.BUFFER_REQUEST, false)
                .baseUri(server.getURI())
                .build()) {

                long responded = client.createRequest(Method.POST)
                    .appendPathPart("/count")
                    .body(new LongInputStream(count))
                    .header("Accept", "application/json")
                    .execute().toCompletableFuture()
                    .get(1, TimeUnit.MINUTES)
                    .body(Long.class).toCompletableFuture()
                    .get(1, TimeUnit.MINUTES);
                Assertions.assertEquals(count, responded);
            }
        }
    }

    private static class LongInputStream extends InputStream {
        private long remaining;

        public LongInputStream(long count) {
            this.remaining = count;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            remaining -= len;
            if (remaining >= 0) {
                return len;
            } else {
                return (int) (len + remaining);
            }
        }
    }

    @Controller("/count")
    @Requires(property = "spec.name", value = "LargeTransferTest")
    public static class Ctrl {
        @Post
        @ExecuteOn(TaskExecutors.BLOCKING)
        int count(@Body InputStream body) throws IOException {
            byte[] buf = new byte[1024];
            int total = 0;
            while (true) {
                int n = body.read(buf);
                if (n == -1) {
                    break;
                }
                if (total / 1_000_000 != (total + n) / 1_000_000) {
                    //System.out.println(total);
                }
                total += n;
            }
            return total;
        }
    }
}
