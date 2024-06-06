package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.Method;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.InetNameResolver;
import io.netty.resolver.InetSocketAddressResolver;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TlsTest {
    @Test
    public void badSsl() {
        for (String uri : Arrays.asList(
                "https://expired.badssl.com/",
                "https://wrong.host.badssl.com/",
                "https://self-signed.badssl.com/",
                "https://untrusted-root.badssl.com/",
                //"https://revoked.badssl.com/", not implemented
                //"https://pinning-test.badssl.com/", not implemented
                "https://no-subject.badssl.com/",
                "https://reversed-chain.badssl.com/",
                "https://rc4-md5.badssl.com/",
                "https://rc4.badssl.com/",
                "https://3des.badssl.com/",
                "https://null.badssl.com/",
                "https://dh480.badssl.com/",
                "https://dh512.badssl.com/",
                "https://dh1024.badssl.com/",
                "https://dh-small-subgroup.badssl.com/",
                "https://dh-composite.badssl.com/"
        )) {
            ExecutionException e = null;
            for (int i = 0; i < 5; i++) {
                try (HttpClient client = new NettyHttpProvider().newBuilder()
                    .baseUri(URI.create(uri))
                    .build()) {
                    e = Assertions.assertThrows(
                        ExecutionException.class,
                        () -> client.createRequest(Method.GET).execute().toCompletableFuture().get(),
                        uri);
                    if (!e.toString().contains("timed out")) {
                        break;
                    }
                    e.printStackTrace();
                }
            }
            Assertions.assertTrue(e.getCause().getCause() instanceof SSLHandshakeException);
        }
    }

    /**
     * AddressResolverGroup that returns a fixed address for a fixed host. The tests use this to make *.example.com
     * resolve to 127.0.0.1
     */
    private static class MockAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {
        private final String expectedHost;
        private final InetAddress address;

        MockAddressResolverGroup(String expectedHost, InetAddress address) {
            this.expectedHost = expectedHost;
            this.address = address;
        }

        @Override
        protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
            return new InetSocketAddressResolver(executor, new InetNameResolver(executor) {
                @Override
                protected void doResolve(String inetHost, Promise<InetAddress> promise) {
                    Assertions.assertEquals(expectedHost, inetHost);
                    promise.setSuccess(address);
                }

                @Override
                protected void doResolveAll(String inetHost, Promise<List<InetAddress>> promise) {
                    Assertions.assertEquals(expectedHost, inetHost);
                    promise.setSuccess(Collections.singletonList(address));
                }
            });
        }
    }
}
