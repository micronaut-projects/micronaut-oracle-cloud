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
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.http.client.ClientProperty;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import com.oracle.bmc.http.client.StandardClientProperties;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.netty.ConnectionManager;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.netty.buffer.ByteBufAllocator;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

final class NettyHttpClient implements HttpClient {
    /**
     * Default settings of {@link com.oracle.bmc.ClientConfiguration}. They are set by BaseClient,
     * so we ignore them if they are the default value.
     */
    private static final Map<ClientProperty<?>, Object> EXPECTED_PROPERTIES;

    final boolean hasContext;
    final URI baseUri;
    final List<RequestInterceptor> requestInterceptors;
    final ExecutorService blockingIoExecutor;
    final String host;
    final int port;
    final boolean buffered;
    final Closeable upstreamHttpClient;
    final ConnectionManager connectionManager;
    final DefaultHttpClient.RequestKey requestKey;

    static {
        ClientConfiguration cfg = ClientConfiguration.builder().build();
        EXPECTED_PROPERTIES = Map.of(
            StandardClientProperties.CONNECT_TIMEOUT, Duration.ofMillis(cfg.getConnectionTimeoutMillis()),
            StandardClientProperties.READ_TIMEOUT, Duration.ofMillis(cfg.getReadTimeoutMillis()),
            StandardClientProperties.ASYNC_POOL_SIZE, cfg.getMaxAsyncThreads()
        );
    }

    NettyHttpClient(NettyHttpClientBuilder builder) {
        DefaultHttpClient mnClient;
        if (builder.managedProvider == null) {
            hasContext = false;
            DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();
            if (builder.properties.containsKey(StandardClientProperties.CONNECT_TIMEOUT)) {
                cfg.setConnectTimeout((Duration) builder.properties.get(StandardClientProperties.CONNECT_TIMEOUT));
            }
            if (builder.properties.containsKey(StandardClientProperties.READ_TIMEOUT)) {
                cfg.setConnectTimeout((Duration) builder.properties.get(StandardClientProperties.READ_TIMEOUT));
            }
            mnClient = new DefaultHttpClient((URI) null, cfg);
            blockingIoExecutor = Executors.newCachedThreadPool();
        } else {
            hasContext = true;
            for (Map.Entry<ClientProperty<?>, Object> entry : builder.properties.entrySet()) {
                if (!entry.getValue().equals(EXPECTED_PROPERTIES.get(entry.getKey()))) {
                    throw new IllegalArgumentException("Cannot change property " + entry.getKey() + " in the managed netty HTTP client. Please configure this setting through the micronaut HTTP client configuration instead. The service ID for the netty client is '" + ManagedNettyHttpProvider.SERVICE_ID + "'.");
                }
            }
            mnClient = (DefaultHttpClient) builder.managedProvider.mnHttpClient;
            blockingIoExecutor = builder.managedProvider.ioExecutor;
        }
        upstreamHttpClient = mnClient;
        connectionManager = mnClient.connectionManager();
        baseUri = Objects.requireNonNull(builder.baseUri, "baseUri");
        requestInterceptors = builder.requestInterceptors.stream()
            .sorted(Comparator.comparingInt(p -> p.priority))
            .map(p -> p.value)
            .collect(Collectors.toList());
        requestKey = new DefaultHttpClient.RequestKey(mnClient, builder.baseUri);
        this.port = builder.baseUri.getPort();
        this.host = builder.baseUri.getHost();
        this.buffered = builder.buffered;
    }

    ByteBufAllocator alloc() {
        return connectionManager.alloc();
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
        if (!hasContext) {
            try {
                upstreamHttpClient.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            blockingIoExecutor.shutdown();
        }
    }
}
