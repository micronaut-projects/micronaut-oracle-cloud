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
package io.micronaut.oraclecloud.certificates.ssl;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Primary;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpVersion;
import io.micronaut.http.client.HttpVersionSelection;
import io.micronaut.http.client.netty.ssl.ClientSslBuilder;
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder;
import io.micronaut.http.netty.NettyTlsUtils;
import io.micronaut.http.server.netty.ssl.ServerSslBuilder;
import io.micronaut.http.ssl.ClientAuthentication;
import io.micronaut.http.ssl.SslConfiguration;
import io.micronaut.oraclecloud.certificates.config.CertificateClientSslConfiguration;
import io.micronaut.oraclecloud.certificates.events.CertificateEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Netty implementation of {@link ServerSslBuilder} that generates an {@link SslContext} to create a server handler
 * with to SSL support via a temporary self signed certificate that will be replaced by an Oracle Cloud certificate once acquired.
 */
@Singleton
@Primary
@BootstrapContextCompatible
public class OracleCloudClientSSLContextBuilder implements ClientSslBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudClientSSLContextBuilder.class);


    private final NettyClientSslBuilder nettyClientSslBuilder;
    private final Map<String, DelegatedSslContext> sslContexts = new ConcurrentHashMap<>();
    private final Map<String, HttpVersionSelection> httpVersionSelectionMap = new ConcurrentHashMap<>();
    private final List<CertificateClientSslConfiguration> sslConfigurations;

    public OracleCloudClientSSLContextBuilder (
        @Nullable NettyClientSslBuilder nettyClientSslBuilder,
        @Nullable List<CertificateClientSslConfiguration> sslConfigurations) {
        this.nettyClientSslBuilder = nettyClientSslBuilder;
        this.sslConfigurations = sslConfigurations;
    }

    /**
     * Listens for CertificateEvent containing the Oracle Cloud certificate and replaces the {@link SslContext} to now use that certificate.
     *
     * @param certificateEvent {@link CertificateEvent}
     */
    @EventListener
    void onNewCertificate(CertificateEvent certificateEvent) {
        String certificateId = certificateEvent.bundle().getCertificateId();
        for (CertificateClientSslConfiguration sslConfiguration : sslConfigurations) {
            if (sslConfiguration.getCertificateConfiguration().certificateId().equals(certificateId)) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("New certificate {} received and replaced the proxied HTTP client [{}] SSL context", certificateEvent.bundle().getCertificateId(), sslConfiguration.getName());
                    }

                    DelegatedSslContext delegatedSslContext = this.sslContexts.computeIfAbsent(certificateId, x -> new DelegatedSslContext(null));
                    HttpVersionSelection versionSelection = this.httpVersionSelectionMap.computeIfAbsent(certificateId, x -> HttpVersionSelection.forLegacyVersion(HttpVersion.HTTP_1_1));

                    List<X509Certificate> chain = new ArrayList<>();
                    chain.add(certificateEvent.certificate());
                    chain.addAll(certificateEvent.intermediate());

                    SslContextBuilder sslBuilder = SslContextBuilder
                        .forClient().keyManager(certificateEvent.privateKey(), chain.toArray(new X509Certificate[]{}))
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .sslProvider(NettyTlsUtils.sslProvider(sslConfiguration));

                    if (sslConfiguration.isInsecureTrustAllCertificates()) {
                        sslBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
                    } else {
                        sslBuilder.trustManager(chain.toArray(new X509Certificate[]{}));
                    }

                    Optional<String[]> protocols = sslConfiguration.getProtocols();
                    protocols.ifPresent(sslBuilder::protocols);

                    Optional<String[]> ciphers = sslConfiguration.getCiphers();

                    if (ciphers.isPresent()) {
                        sslBuilder = sslBuilder.ciphers(Arrays.asList(ciphers.get()));
                    } else if (versionSelection.isHttp2CipherSuites()) {
                        sslBuilder.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE);
                    }

                    Optional<ClientAuthentication> clientAuthentication = sslConfiguration.getClientAuthentication();
                    if (clientAuthentication.isPresent()) {
                        ClientAuthentication clientAuth = clientAuthentication.get();
                        if (clientAuth == ClientAuthentication.NEED) {
                            sslBuilder = sslBuilder.clientAuth(ClientAuth.REQUIRE);
                        } else if (clientAuth == ClientAuthentication.WANT) {
                            sslBuilder = sslBuilder.clientAuth(ClientAuth.OPTIONAL);
                        }
                    }

                    if (versionSelection.isAlpn()) {
                        SslProvider provider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
                        sslBuilder.sslProvider(provider);
                        sslBuilder.applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN, ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE, ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, versionSelection.getAlpnSupportedProtocols()));
                    }

                    delegatedSslContext.setNewSslContext(sslBuilder.build());
                } catch (SSLException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to build the SSL context: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    @Override
    public @NonNull SslContext build(SslConfiguration ssl, HttpVersionSelection versionSelection) {
        if (ssl instanceof CertificateClientSslConfiguration ccSslConfiguration) {
            String certificateId = ccSslConfiguration.getCertificateConfiguration().certificateId();
            this.httpVersionSelectionMap.computeIfAbsent(certificateId, x-> versionSelection);
            return this.sslContexts.computeIfAbsent(certificateId, x -> new DelegatedSslContext(null));
        } else {
            return this.nettyClientSslBuilder.build(ssl, versionSelection);
        }
    }

}
