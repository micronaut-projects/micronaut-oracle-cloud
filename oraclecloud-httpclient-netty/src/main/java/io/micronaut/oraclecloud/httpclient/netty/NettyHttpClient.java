/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.httpclient.netty;

import com.fasterxml.jackson.core.JacksonException;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

final class NettyHttpClient implements HttpClient {
    final URI baseUri;
    final List<RequestInterceptor> requestInterceptors;
    final Bootstrap bootstrap;
    final NioEventLoopGroup group;
    final ExecutorService blockingIoExecutor;
    final SslContext sslContext;
    final String host;
    final int port;
    final boolean buffered;

    NettyHttpClient(NettyHttpClientBuilder builder) {
        baseUri = Objects.requireNonNull(builder.baseUri, "baseUri");
        requestInterceptors = builder.requestInterceptors.stream()
                .sorted(Comparator.comparingInt(p -> p.priority))
                .map(p -> p.value)
                .collect(Collectors.toList());
        int defaultPort;
        if (builder.baseUri.getScheme().equalsIgnoreCase("http")) {
            defaultPort = 80;
            sslContext = null;
        } else {
            defaultPort = 443;
            try {
                if (builder.sslContext == null) {
                    SslContextBuilder sslBuilder = SslContextBuilder.forClient();
                    if (builder.keyStore != null) {
                        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
                        kmf.init(new KeyStoreBuilderParameters(KeyStore.Builder.newInstance(
                                builder.keyStore.getKeyStore(),
                                new KeyStore.PasswordProtection(builder.keyStore.getPassword().toCharArray())
                        )));
                        sslBuilder.keyManager(kmf);
                    }

                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(builder.trustStore);
                    if (builder.hostnameVerifier != null) {
                        tmf = new CustomTrustManagerFactory(tmf, builder.hostnameVerifier);
                    }
                    sslBuilder.trustManager(tmf);

                    sslContext = sslBuilder.build();
                } else {
                    sslContext = new JdkSslContext(
                            builder.sslContext,
                            true,
                            null,
                            IdentityCipherSuiteFilter.INSTANCE,
                            null,
                            ClientAuth.NONE, // ignored for isClient=true
                            null,
                            false
                    );
                }
            } catch (SSLException | GeneralSecurityException e) {
                throw new IllegalStateException("Couldn't set up ssl context", e);
            }
        }
        int port = builder.baseUri.getPort();
        if (port == -1) {
            port = defaultPort;
        }
        this.port = port;
        this.host = builder.baseUri.getHost();
        bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(builder.connectTimeout.toMillis()))
                .remoteAddress(host, port);

        group = new NioEventLoopGroup(builder.asyncPoolSize);
        bootstrap.group(group);
        blockingIoExecutor = Executors.newCachedThreadPool();
        this.buffered = builder.buffered;
    }

    ByteBufAllocator alloc() {
        return (ByteBufAllocator) bootstrap.config().options().getOrDefault(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
    }

    @Override
    public HttpRequest createRequest(Method method) {
        return new NettyHttpRequest(this, method);
    }

    @Override
    public boolean isProcessingException(Exception e) {
        return e instanceof JacksonException;
    }

    @Override
    public void close() {
        group.shutdownGracefully();
        blockingIoExecutor.shutdown();
    }
}
