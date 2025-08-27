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

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.netty.NettyTlsUtils;
import io.micronaut.http.server.netty.ssl.CertificateProvidedSslBuilder;
import io.micronaut.http.server.netty.ssl.ServerSslBuilder;
import io.micronaut.http.ssl.ClientAuthentication;
import io.micronaut.http.ssl.ServerSslConfiguration;
import io.micronaut.oraclecloud.certificates.config.DefaultServerOracleCloudCertificateConfiguration;
import io.micronaut.oraclecloud.certificates.config.ServerOracleCloudCertificateConfiguration;
import io.micronaut.oraclecloud.certificates.events.CertificateEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The Netty implementation of {@link ServerSslBuilder} that generates an {@link SslContext} to create a server handler
 * with to SSL support via a temporary self signed certificate that will be replaced by an Oracle Cloud certificate once acquired.
 */
@Singleton
@Replaces(CertificateProvidedSslBuilder.class)
@Requires(beans = DefaultServerOracleCloudCertificateConfiguration.class)
public final class OracleCloudSSLContextBuilder implements ServerSslBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudSSLContextBuilder.class);

    private final DelegatedSslContext delegatedSslContext = new DelegatedSslContext(null);
    private final ServerSslConfiguration ssl;
    private final ServerOracleCloudCertificateConfiguration configuration;

    /**
     * @param ssl The SSL configuration
     */
    @Deprecated
    public OracleCloudSSLContextBuilder(ServerSslConfiguration ssl) {
        this.ssl = ssl;
        this.configuration = null;
    }

    /**
     * @param configuration The server configuration
     * @param ssl           The SSL configuration
     */
    @Inject
    public OracleCloudSSLContextBuilder(ServerOracleCloudCertificateConfiguration configuration, ServerSslConfiguration ssl) {
        this.ssl = ssl;
        this.configuration = configuration;
    }

    /**
     * Listens for CertificateEvent containing the Oracle Cloud certificate and replaces the {@link SslContext} to now use that certificate.
     *
     * @param certificateEvent {@link CertificateEvent}
     */
    @EventListener
    void onNewCertificate(CertificateEvent certificateEvent) {
        if (configuration != null && configuration.certificateId().equals(certificateEvent.bundle().getCertificateId())) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("New certificate {} received and replaced the proxied HTTP server SSL context", certificateEvent.bundle().getCertificateId());
                }
                List<X509Certificate> chain = new ArrayList<>();
                chain.add(certificateEvent.certificate());
                chain.addAll(certificateEvent.intermediate());
                SslContextBuilder sslContextBuilder = SslContextBuilder
                    .forServer(certificateEvent.privateKey(), chain.toArray(new X509Certificate[]{}))
                    .sslProvider(NettyTlsUtils.sslProvider(ssl));

                Optional<String[]> protocols = ssl.getProtocols();
                protocols.ifPresent(sslContextBuilder::protocols);

                Optional<String[]> ciphers = ssl.getCiphers();

                ciphers.ifPresent(strings -> sslContextBuilder.ciphers(Arrays.asList(strings)));

                Optional<ClientAuthentication> clientAuthentication = ssl.getClientAuthentication();
                if (clientAuthentication.isPresent()) {
                    ClientAuthentication clientAuth = clientAuthentication.get();
                    if (clientAuth == ClientAuthentication.NEED) {
                        sslContextBuilder.clientAuth(ClientAuth.REQUIRE);
                    } else if (clientAuth == ClientAuthentication.WANT) {
                        sslContextBuilder.clientAuth(ClientAuth.OPTIONAL);
                    }
                }

                delegatedSslContext.setNewSslContext(sslContextBuilder.build());
            } catch (SSLException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to build the SSL context: " + e.getMessage(), e);
                }
            }
        }

    }

    @Override
    public ServerSslConfiguration getSslConfiguration() {
        return ssl;
    }

    /**
     * Generates an SslContext that has an already expired self signed cert that should be replaced almost immediately by the Oracle Cloud Certificate server once it is downloaded.
     *
     * @return Optional SslContext
     */
    @Override
    public Optional<SslContext> build() {
        return Optional.of(delegatedSslContext);
    }
}
