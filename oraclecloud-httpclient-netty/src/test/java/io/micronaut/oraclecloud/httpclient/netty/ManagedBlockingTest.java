package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.internal.ClientCall;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.requests.BmcRequest;
import com.oracle.bmc.responses.BmcResponse;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "ManagedBlockingTest",
            "micronaut.server.port", 0
        )); EmbeddedServer server = ctx.getBean(EmbeddedServer.class)) {
            server.start();
            NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
            try (com.oracle.bmc.http.client.HttpClient cl = new NettyHttpProvider().newBuilder()
                .baseUri(server.getURI())
                .build()) {
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
