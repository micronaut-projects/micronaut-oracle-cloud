/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.oraclecloud.oke.workload.identity;

import com.oracle.bmc.auth.ServiceAccountTokenSupplier;
import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeTenancyOnlyAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeWorkloadIdentityResourcePrincipalsFederationClient;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.Priorities;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpProviderCapability;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.internal.AuthnClientFilter;
import com.oracle.bmc.http.internal.ClientIdFilter;
import com.oracle.bmc.http.internal.LogHeadersFilter;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.util.internal.StringUtils;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.client.netty.DefaultHttpClientBuilder;
import io.micronaut.oraclecloud.httpclient.netty.ManagedNettyHttpProvider;
import io.micronaut.oraclecloud.httpclient.netty.OciNettyClientFilter;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.micronaut.oraclecloud.oke.workload.identity.MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.getOkeHttpClientConfiguration;

final class MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient
    extends OkeWorkloadIdentityResourcePrincipalsFederationClient implements AutoCloseable {

    public static final String KUBERNETES_SERVICE_ACCOUNT_ERROR_MESSAGE = "Kubernetes service account ca cert doesn't exist.";

    static final String KUBERNETES_SERVICE_ACCOUNT_CERT_PATH =
        "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

    private final List<OciNettyClientFilter<?>> nettyClientFilters;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Constructor of OkeWorkloadIdentityResourcePrincipalsFederationClient.
     *
     * @param sessionKeySupplier                          the session key supplier.
     * @param okeTenancyOnlyAuthenticationDetailsProvider the key pair authentication details
     *                                                    provider.
     * @param clientConfigurator                          the reset client configurator.
     * @param circuitBreakerConfiguration
     * @param additionalClientConfigurators
     */
    public MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient(
        SessionKeySupplier sessionKeySupplier,
        ServiceAccountTokenSupplier serviceAccountTokenSupplier,
        OkeTenancyOnlyAuthenticationDetailsProvider okeTenancyOnlyAuthenticationDetailsProvider,
        ClientConfigurator clientConfigurator,
        CircuitBreakerConfiguration circuitBreakerConfiguration,
        List<ClientConfigurator> additionalClientConfigurators,
        List<OciNettyClientFilter<?>> nettyClientFilters
    ) {
        super(sessionKeySupplier, serviceAccountTokenSupplier, okeTenancyOnlyAuthenticationDetailsProvider, clientConfigurator, circuitBreakerConfiguration, additionalClientConfigurators);
        this.nettyClientFilters = nettyClientFilters;
    }

    public static OkeNettyClientSslBuilder okeNettyClientSslBuilder(String path) {
        Path pathToFile = Paths.get(path);
        if (Files.exists(pathToFile)) {
            try (InputStream inputStream = new FileInputStream(pathToFile.toFile())) {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate =
                    (X509Certificate) certFactory.generateCertificate(inputStream);

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                keyStore.setCertificateEntry("ocp-cert", certificate);
                tmf.init(keyStore);
                return new OkeNettyClientSslBuilder(new ResourceResolver(), tmf, keyStore);
            } catch (CertificateException e) {
                throw new IllegalArgumentException(
                    "Invalid Kubernetes ca certification. Please contact OKE Foundation team for help.",
                    e);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(
                    KUBERNETES_SERVICE_ACCOUNT_ERROR_MESSAGE, e);
            } catch (KeyStoreException e) {
                throw new IllegalArgumentException(
                    "Cannot create keystore based on Kubernetes ca cert. Please contact OKE Foundation team for help.",
                    e);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                    "Cannot load keystore. Please contact OKE Foundation team for help.",
                    e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(
                    "Cannot load keystore. Please contact OKE Foundation team for help.",
                    e);
            }
        } else {
            throw new IllegalArgumentException(KUBERNETES_SERVICE_ACCOUNT_ERROR_MESSAGE);
        }
    }

    DefaultHttpClient defaultHttpClient() {
        // Set ca cert when talking to proxymux using https.
        DefaultHttpClientBuilder builder = DefaultHttpClient.builder();
        OkeHttpClientConfiguration configuration = getOkeHttpClientConfiguration();
        if (configuration != null) {
            builder.configuration(configuration);
        }
        return builder
            .nettyClientSslBuilder(okeNettyClientSslBuilder(KUBERNETES_SERVICE_ACCOUNT_CERT_PATH))
            .build();
    }

    @Override
    @SuppressWarnings("java:S1181") // Release owned resources even when client initialization throws an Error.
    protected HttpClient makeClient(String endpoint, RequestSigner requestSigner) {
        if (StringUtils.isBlank(endpoint)) {
            // this is the case for the federation client, which isn't used in OKE workload identity
            return null;
        }

        DefaultHttpClient rawHttpClient = null;
        ExecutorService ioExecutor = null;
        try {
            rawHttpClient = defaultHttpClient();
            ioExecutor = Executors.newCachedThreadPool();
            HttpClientBuilder rptBuilder = new ManagedNettyHttpProvider(
                    rawHttpClient,
                    ioExecutor,
                    nettyClientFilters)
                    .newBuilder()
                    .baseUri(URI.create(endpoint))
                    .registerRequestInterceptor(
                        Priorities.AUTHENTICATION,
                        new AuthnClientFilter(requestSigner, Collections.emptyMap()))
                    .registerRequestInterceptor(
                        Priorities.HEADER_DECORATOR, new ClientIdFilter())
                    .registerRequestInterceptor(Priorities.USER, new LogHeadersFilter());

            if (clientConfigurator != null) {
                clientConfigurator.customizeClient(rptBuilder);
            }

            for (ClientConfigurator additionalConfigurator : additionalClientConfigurator) {
                additionalConfigurator.customizeClient(rptBuilder);
            }

            return new OwnedHttpClient(rptBuilder.build(), rawHttpClient, ioExecutor);
        } catch (RuntimeException | Error e) {
            closeOwnedResources(rawHttpClient, ioExecutor, e);
            throw e;
        }
    }

    /**
     * Closes the HTTP clients and executor created by this federation client.
     *
     * <p>The SDK does not expose a close contract for {@code FederationClient}. The managed OCI
     * client also deliberately leaves the explicitly supplied Micronaut HTTP client and executor
     * open, so this owner closes both resources when the authentication bean is destroyed.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Throwable failure = closeResource(resourcePrincipalTokenClient, null);
        if (federationClient != resourcePrincipalTokenClient) {
            failure = closeResource(federationClient, failure);
        }
        propagateFailure(failure);
    }

    boolean isClosed() {
        return closed.get();
    }

    static HttpClient ownedHttpClient(
        HttpClient delegate,
        AutoCloseable rawHttpClient,
        ExecutorService ioExecutor) {
        return new OwnedHttpClient(delegate, rawHttpClient, ioExecutor);
    }

    static void closeOwnedResources(
        @Nullable AutoCloseable rawHttpClient,
        @Nullable ExecutorService ioExecutor,
        Throwable failure) {
        closeResource(ioExecutor == null ? null : ioExecutor::shutdownNow, failure);
        closeResource(rawHttpClient, failure);
    }

    @SuppressWarnings("java:S1181") // Every owned resource must close, including after another close throws an Error.
    private static @Nullable Throwable closeResource(@Nullable AutoCloseable resource, @Nullable Throwable failure) {
        if (resource == null) {
            return failure;
        }
        try {
            resource.close();
        } catch (Throwable e) {
            if (failure == null) {
                return e;
            }
            if (failure != e) {
                failure.addSuppressed(e);
            }
        }
        return failure;
    }

    private static void propagateFailure(@Nullable Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure != null) {
            throw new IllegalStateException("Failed to close OKE federation HTTP resources", failure);
        }
    }

    private static final class OwnedHttpClient implements HttpClient {
        private final HttpClient delegate;
        private final AutoCloseable rawHttpClient;
        private final ExecutorService ioExecutor;
        private final AtomicBoolean closed = new AtomicBoolean();

        private OwnedHttpClient(
            HttpClient delegate,
            AutoCloseable rawHttpClient,
            ExecutorService ioExecutor) {
            this.delegate = delegate;
            this.rawHttpClient = rawHttpClient;
            this.ioExecutor = ioExecutor;
        }

        @Override
        public HttpRequest createRequest(Method method) {
            return delegate.createRequest(method);
        }

        @Override
        public HttpRequest createRequest(
            Method method,
            Map<String, Object> requiredParametersMap,
            Map<String, Boolean> optionsMap) {
            return delegate.createRequest(method, requiredParametersMap, optionsMap);
        }

        @Override
        public boolean isProcessingException(Exception e) {
            return delegate.isProcessingException(e);
        }

        @Override
        public boolean hasCapability(HttpProviderCapability capability) {
            return delegate.hasCapability(capability);
        }

        @Override
        public void updateEndpoint(String baseTarget) {
            delegate.updateEndpoint(baseTarget);
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            Throwable failure = closeResource(delegate, null);
            failure = closeResource(ioExecutor::shutdown, failure);
            failure = closeResource(rawHttpClient, failure);
            propagateFailure(failure);
        }
    }
}
