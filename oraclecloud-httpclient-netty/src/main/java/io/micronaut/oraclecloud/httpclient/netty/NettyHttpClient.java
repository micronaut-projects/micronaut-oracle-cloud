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

import tools.jackson.core.JacksonException;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.http.client.ClientProperty;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import com.oracle.bmc.http.client.StandardClientProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpVersionSelection;
import io.micronaut.http.client.RawHttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.ResponseClosedException;
import io.micronaut.http.client.netty.ConnectionManager;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.json.JsonMapper;
import io.micronaut.oraclecloud.serde.OciSdkMicronautSerializer;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.util.concurrent.FastThreadLocalThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.micronaut.oraclecloud.httpclient.netty.NettyClientProperties.OCI_NETTY_CLIENT_FILTERS_KEY;

@Internal
final class NettyHttpClient implements HttpClient {
    /**
     * Default settings of {@link ClientConfiguration}. They are set by BaseClient,
     * so we ignore them if they are the default value.
     */
    private static final Map<ClientProperty<?>, Object> EXPECTED_PROPERTIES;

    private static final boolean LEGACY_NETTY_CLIENT = Boolean.getBoolean("io.micronaut.oraclecloud.httpclient.netty.legacy-netty-client");

    private static final Logger LOG = LoggerFactory.getLogger(NettyHttpClient.class);
    private static final String BLOCKING_EVENT_LOOP_MESSAGE = "You are trying to run a BlockingHttpClient operation on a netty event "
        + "loop thread. This is a common cause for bugs: Event loops should never be blocked. "
        + "You can either mark your controller as @ExecuteOn(TaskExecutors.BLOCKING), or use the reactive HTTP client "
        + "to resolve this bug. There is also a configuration option to disable this check if you are certain a "
        + "blocking operation is fine here.";

    final boolean legacyNettyClient;
    final boolean hasContext;
    final boolean ownsThreadPool;
    final String baseUri;
    volatile ThreadLocal<URI> localBaseUri = null;
    final List<RequestInterceptor> requestInterceptors;
    final List<OciNettyClientFilter<?>> nettyClientFilter;
    final ExecutorService blockingIoExecutor;
    final boolean buffered;
    final ConnectionManager connectionManager;
    final RawHttpClient upstreamHttpClient;
    final JsonMapper jsonMapper;

    static {
        ClientConfiguration cfg = ClientConfiguration.builder().build();
        EXPECTED_PROPERTIES = Map.of(
            StandardClientProperties.CONNECT_TIMEOUT, Duration.ofMillis(cfg.getConnectionTimeoutMillis()),
            StandardClientProperties.READ_TIMEOUT, Duration.ofMillis(cfg.getReadTimeoutMillis()),
            StandardClientProperties.ASYNC_POOL_SIZE, cfg.getMaxAsyncThreads()
        );
    }

    NettyHttpClient(NettyHttpClientBuilder builder) {
        this.legacyNettyClient = LEGACY_NETTY_CLIENT || (builder.managedProvider != null && builder.managedProvider.configuration.legacyNettyClient());
        RawHttpClient mnClient;
        if (builder.managedProvider == null) {
            hasContext = false;
            ownsThreadPool = true;
            DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();
            // Configure proxy via system properties for OCI use-cases
            applyProxyFromSystemProperties(cfg, LOG);

            if (builder.properties.containsKey(StandardClientProperties.CONNECT_TIMEOUT)) {
                cfg.setConnectTimeout((Duration) builder.properties.get(StandardClientProperties.CONNECT_TIMEOUT));
            }
            if (builder.properties.containsKey(StandardClientProperties.READ_TIMEOUT)) {
                cfg.setReadTimeout((Duration) builder.properties.get(StandardClientProperties.READ_TIMEOUT));
            }
            mnClient = RawHttpClient.create(null, cfg);
            blockingIoExecutor = Executors.newCachedThreadPool();
            jsonMapper = OciSdkMicronautSerializer.getDefaultObjectMapper();
        } else {
            hasContext = true;
            for (Map.Entry<ClientProperty<?>, Object> entry : builder.properties.entrySet()) {
                if (!entry.getValue().equals(EXPECTED_PROPERTIES.get(entry.getKey())) && !entry.getKey().equals(OCI_NETTY_CLIENT_FILTERS_KEY)) {
                    LOG.debug("Ignoring invalid property {}:{} in the managed netty HTTP client. Please configure this setting through the micronaut HTTP client configuration instead. The service ID for the netty client is {}", entry.getKey(), entry.getValue(), ManagedNettyHttpProvider.SERVICE_ID);
                }
            }
            if (builder.managedProvider.mnHttpClient != null) {
                mnClient = builder.managedProvider.mnHttpClient;
            } else {
                mnClient = builder.managedProvider.mnHttpClientRegistry.getRawClient(
                    HttpVersionSelection.forClientConfiguration(new DefaultHttpClientConfiguration()),
                    builder.serviceId,
                    null
                );
            }
            if (builder.managedProvider.ioExecutor == null) {
                ownsThreadPool = true;
                blockingIoExecutor = Executors.newCachedThreadPool();
            } else {
                ownsThreadPool = false;
                blockingIoExecutor = builder.managedProvider.ioExecutor;
            }
            jsonMapper = builder.managedProvider.jsonMapper;
        }
        upstreamHttpClient = mnClient;
        connectionManager = legacyNettyClient ? ((DefaultHttpClient) mnClient).connectionManager() : null;
        baseUri = Objects.requireNonNull(builder.baseUri, "baseUri");
        requestInterceptors = builder.requestInterceptors.stream()
            .sorted(Comparator.comparingInt(p -> p.priority))
            .map(p -> p.value)
            .collect(Collectors.toList());

        if (builder.properties.containsKey(OCI_NETTY_CLIENT_FILTERS_KEY)) {
            nettyClientFilter = ((List<OciNettyClientFilter<?>>) builder.properties.get(OCI_NETTY_CLIENT_FILTERS_KEY)).stream().sorted(OrderUtil.COMPARATOR).toList();
        } else {
            nettyClientFilter = Collections.emptyList();
        }

        this.buffered = builder.buffered;
    }

    ByteBufAllocator alloc() {
        return connectionManager == null ? ByteBufAllocator.DEFAULT : connectionManager.alloc();
    }

    static boolean isBlockingOperationOnEventLoop(Executor offloadExecutor) {
        return offloadExecutor != null && Thread.currentThread() instanceof FastThreadLocalThread;
    }

    static HttpClientException blockingOperationOnEventLoopException() {
        return new HttpClientException(BLOCKING_EVENT_LOOP_MESSAGE);
    }

    String baseUri() {
        ThreadLocal<URI> localBaseUri = this.localBaseUri;
        if (localBaseUri != null) {
            URI loc = localBaseUri.get();
            if (loc != null) {
                return loc.toString();
            }
        }
        return baseUri;
    }

    @SuppressWarnings("deprecation")
    @Override
    public HttpRequest createRequest(Method method) {
        return legacyNettyClient ? new NettyHttpRequest(this, method) : new MicronautHttpRequest(this, method);
    }

    @Override
    public boolean isProcessingException(Exception e) {
        // these exceptions will allow the client to retry the request
        return e instanceof JacksonException ||
            e instanceof PrematureChannelClosureException ||
            e instanceof ChannelException ||
            e instanceof io.micronaut.http.client.exceptions.ReadTimeoutException ||
            e instanceof ResponseClosedException ||
            e instanceof SocketException;
    }

    @Override
    public void close() {
        if (!hasContext) {
            try {
                upstreamHttpClient.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (ownsThreadPool) {
            blockingIoExecutor.shutdown();
        }
    }

    @Override
    public void updateEndpoint(String baseTarget) {
        ThreadLocal<URI> localBaseUri = this.localBaseUri;
        if (localBaseUri == null) {
            synchronized (this) {
                localBaseUri = this.localBaseUri;
                if (localBaseUri == null) {
                    localBaseUri = new ThreadLocal<>();
                    this.localBaseUri = localBaseUri;
                }
            }
        }
        localBaseUri.set(URI.create(baseTarget));
    }

    /**
     * Apply proxy configuration from OCI-specific system properties to the given Micronaut HTTP client configuration.
     * Recognized properties:
     * - io.micronaut.oci.proxy (URI, e.g. http://user:pass@host:port or socks5://host:1080)
     * - io.micronaut.oci.proxy.host
     * - io.micronaut.oci.proxy.port
     * - io.micronaut.oci.proxy.username
     * - io.micronaut.oci.proxy.password
     * - io.micronaut.oci.proxy.type (http|socks|socks5)
     * - io.micronaut.oci.proxy.nonProxyHosts (comma-separated, supports * wildcard)
     */
    @SuppressWarnings("java:S3776")
    static void applyProxyFromSystemProperties(DefaultHttpClientConfiguration cfg, Logger log) {
        try {
            final String proxyProp = System.getProperty("io.micronaut.oci.proxy");
            final String hostProp = System.getProperty("io.micronaut.oci.proxy.host");
            final String portProp = System.getProperty("io.micronaut.oci.proxy.port");
            final String userProp = System.getProperty("io.micronaut.oci.proxy.username");
            final String passProp = System.getProperty("io.micronaut.oci.proxy.password");
            final String typeProp = System.getProperty("io.micronaut.oci.proxy.type");
            final String nonProxyProp = System.getProperty("io.micronaut.oci.proxy.nonProxyHosts");

            Proxy.Type proxyType = Proxy.Type.HTTP;
            if (typeProp != null) {
                if ("socks".equalsIgnoreCase(typeProp) || "socks5".equalsIgnoreCase(typeProp)) {
                    proxyType = Proxy.Type.SOCKS;
                } else {
                    proxyType = Proxy.Type.HTTP;
                }
            }

            String host = null;
            int port = -1;
            String user = null;
            String pass = null;

            if (proxyProp != null && !proxyProp.isBlank()) {
                URI pUri = URI.create(proxyProp.trim());
                if (pUri.getScheme() != null) {
                    if (pUri.getScheme().toLowerCase(Locale.ROOT).startsWith("socks")) {
                        proxyType = Proxy.Type.SOCKS;
                    } else {
                        proxyType = Proxy.Type.HTTP;
                    }
                }
                host = pUri.getHost();
                port = pUri.getPort();
                String ui = pUri.getUserInfo();
                if (ui != null) {
                    int idx = ui.indexOf(':');
                    if (idx >= 0) {
                        user = ui.substring(0, idx);
                        pass = ui.substring(idx + 1);
                    } else {
                        user = ui;
                    }
                }
                if (port < 0) {
                    port = proxyType == Proxy.Type.SOCKS ? 1080 : 80;
                }
            } else if (hostProp != null && !hostProp.isBlank()) {
                host = hostProp.trim();
                if (portProp != null) {
                    try {
                        port = Integer.parseInt(portProp.trim());
                    } catch (NumberFormatException ignored) {
                        port = -1;
                    }
                }
                if (port < 0) {
                    port = proxyType == Proxy.Type.SOCKS ? 1080 : 80;
                }
                user = userProp;
                pass = passProp;
            }

            if (host != null && !host.isBlank()) {
                InetSocketAddress address = new InetSocketAddress(host, port);
                cfg.setProxyType(proxyType);
                cfg.setProxyAddress(address);
                if (user != null && !user.isBlank()) {
                    cfg.setProxyUsername(user);
                }
                if (pass != null && !pass.isBlank()) {
                    cfg.setProxyPassword(pass);
                }
                if (nonProxyProp != null && !nonProxyProp.isBlank()) {
                    final var patterns = Arrays.stream(nonProxyProp.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(p -> p.replace(".", "\\.").replace("*", ".*"))
                        .map(Pattern::compile)
                        .toList();
                    final Proxy proxy = new Proxy(proxyType, address);
                    cfg.setProxySelector(new ProxySelector() {
                        @Override
                        public List<Proxy> select(URI uri) {
                            String h = uri.getHost();
                            if (h != null) {
                                for (Pattern pat : patterns) {
                                    if (pat.matcher(h).matches()) {
                                        return List.of(Proxy.NO_PROXY);
                                    }
                                }
                            }
                            return List.of(proxy);
                        }

                        @Override
                        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                            // no-op
                        }
                    });
                }
            }
        } catch (RuntimeException e) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring OCI proxy system properties due to error: {}", e.getMessage(), e);
            }
        }
    }
}
