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
import com.oracle.bmc.http.internal.AuthnClientFilter;
import com.oracle.bmc.http.internal.ClientIdFilter;
import com.oracle.bmc.http.internal.LogHeadersFilter;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.util.internal.StringUtils;
import io.micronaut.buffer.netty.NettyByteBufferFactory;
import io.micronaut.core.annotation.AnnotationMetadataResolver;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.MediaType;
import io.micronaut.http.body.ContextlessMessageBodyHandlerRegistry;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.http.netty.body.ByteBufRawMessageBodyHandler;
import io.micronaut.http.netty.body.NettyJsonHandler;
import io.micronaut.http.netty.body.NettyJsonStreamHandler;
import io.micronaut.http.netty.body.NettyWritableBodyWriter;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.codec.JsonMediaTypeCodec;
import io.micronaut.json.codec.JsonStreamMediaTypeCodec;
import io.micronaut.oraclecloud.httpclient.netty.ManagedNettyHttpProvider;
import io.micronaut.runtime.ApplicationConfiguration;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

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
import java.util.concurrent.Executors;

import static io.micronaut.oraclecloud.oke.workload.identity.MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.getOkeHttpClientConfiguration;

final class MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient extends OkeWorkloadIdentityResourcePrincipalsFederationClient {

    public static final String KUBERNETES_SERVICE_ACCOUNT_ERROR_MESSAGE = "Kubernetes service account ca cert doesn't exist.";

    static final String KUBERNETES_SERVICE_ACCOUNT_CERT_PATH =
        "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

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
        List<ClientConfigurator> additionalClientConfigurators
    ) {
        super(sessionKeySupplier, serviceAccountTokenSupplier, okeTenancyOnlyAuthenticationDetailsProvider, clientConfigurator, circuitBreakerConfiguration, additionalClientConfigurators);
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
        JsonMapper mapper = JsonMapper.createDefault();
        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        ContextlessMessageBodyHandlerRegistry registry = new ContextlessMessageBodyHandlerRegistry(
            applicationConfiguration,
            NettyByteBufferFactory.DEFAULT,
            new ByteBufRawMessageBodyHandler(),
            new NettyWritableBodyWriter(applicationConfiguration)
        );
        registry.add(MediaType.APPLICATION_JSON_TYPE, new NettyJsonHandler<>(mapper));
        registry.add(MediaType.APPLICATION_JSON_STREAM_TYPE, new NettyJsonStreamHandler<>(mapper));
        // Set ca cert when talking to proxymux using https.

        return new DefaultHttpClient(null,
            getOkeHttpClientConfiguration(), null, new DefaultThreadFactory(MultithreadEventLoopGroup.class),
            okeNettyClientSslBuilder(KUBERNETES_SERVICE_ACCOUNT_CERT_PATH),
            MediaTypeCodecRegistry.of(
                new JsonMediaTypeCodec(mapper, applicationConfiguration, null),
                new JsonStreamMediaTypeCodec(mapper, applicationConfiguration, null)
            ),
            registry,
            AnnotationMetadataResolver.DEFAULT,
            ConversionService.SHARED);
    }

    @Override
    protected HttpClient makeClient(String endpoint, RequestSigner requestSigner) {
        if (StringUtils.isBlank(endpoint)) {
            // this is the case for the federation client, which isn't used in OKE workload identity
            return null;
        }

        HttpClientBuilder rptBuilder = new ManagedNettyHttpProvider(defaultHttpClient(), Executors.newCachedThreadPool(),  JsonMapper.createDefault())
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

        return rptBuilder.build();
    }
}
