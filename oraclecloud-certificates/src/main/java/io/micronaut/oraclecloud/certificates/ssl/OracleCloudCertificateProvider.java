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
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import io.micronaut.http.server.netty.ssl.ServerSslBuilder;
import io.micronaut.http.ssl.CertificateProvider;
import io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateProperties;
import io.micronaut.oraclecloud.certificates.events.CertificateEvent;
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateFetcher;
import io.micronaut.context.event.ApplicationEventListener;
import io.netty.handler.ssl.SslContext;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * The Netty implementation of {@link ServerSslBuilder} that generates an {@link SslContext} to create a server handler
 * with to SSL support via a temporary self signed certificate that will be replaced by an Oracle Cloud certificate once acquired.
 */
@Internal
@EachBean(OracleCloudCertificateProperties.class)
@BootstrapContextCompatible
@Singleton
final class OracleCloudCertificateProvider implements CertificateProvider, ApplicationEventListener<CertificateEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudCertificateProvider.class);

    private final OracleCloudCertificateProperties configuration;

    private final Flux<KeyStore> bundleKeystore;
    private final Flux<KeyStore> bundleTruststore;
    private final Sinks.Many<KeyStore> sinkKeystore;
    private final Sinks.Many<KeyStore> sinkTruststore;

    /**
     * Creates a provider that supplies KeyStore and TrustStore material from OCI Certificates.
     *
     * The constructor attempts to preload any already-available certificate for the provided configuration.
     *
     * @param configuration The OCI certificate configuration for this provider.
     * @param oracleCloudCertificateFetcher Fetcher used to obtain certificate material from OCI.
     * @throws CertificateException If a previously fetched certificate cannot be parsed.
     */
    OracleCloudCertificateProvider(OracleCloudCertificateProperties configuration,
                                   OracleCloudCertificateFetcher oracleCloudCertificateFetcher) throws CertificateException {
        this.configuration = configuration;
        sinkKeystore = Sinks.many().replay().latest();
        sinkTruststore = Sinks.many().replay().latest();
        // Preload any already-fetched certificates
        CertificateEvent certificateEvent = oracleCloudCertificateFetcher.retrieveCertificate(configuration.certificateId(), configuration.versionNumber(), configuration.certificateVersionName());
        if (certificateEvent != null) {
            onApplicationEvent(certificateEvent);
        }
        bundleKeystore = sinkKeystore.asFlux();
        bundleTruststore = sinkTruststore.asFlux();
    }

    /**
     * Handles a newly retrieved certificate by rebuilding the PKCS12 KeyStore and TrustStore.
     *
     * Only events that match this provider's configured certificate OCID are applied.
     *
     * @param certificateEvent The event containing the private key, leaf certificate, and intermediate chain.
     * @throws RuntimeException If a PKCS12 KeyStore cannot be populated from the event.
     */
    @Override
    public void onApplicationEvent(CertificateEvent certificateEvent) {
        try {
            if (configuration != null
                && configuration.certificateId().equals(certificateEvent.bundle().getCertificateId())) {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("New certificate {} received and replaced the KeyStore",
                        certificateEvent.bundle().getCertificateId());
                }

                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, null);

                List<X509Certificate> chain = new ArrayList<>();
                X509Certificate leaf = certificateEvent.certificate();
                chain.add(leaf);
                for (X509Certificate cert : certificateEvent.intermediate()) {
                    if (!cert.equals(leaf)) {
                        chain.add(cert);
                    }
                }

                ks.setKeyEntry(certificateEvent.bundle().getCertificateId(),
                    certificateEvent.privateKey(),
                    new char[0],
                    chain.toArray(new X509Certificate[0]));

                sinkKeystore.tryEmitNext(ks);

                KeyStore trustStore = KeyStore.getInstance("PKCS12");
                trustStore.load(null, null);

                // Add the leaf and intermediates as trusted certs
                trustStore.setCertificateEntry(
                    certificateEvent.bundle().getCertificateId() + "-leaf",
                    leaf
                );

                for (X509Certificate cert : certificateEvent.intermediate()) {
                    trustStore.setCertificateEntry(
                        certificateEvent.bundle().getCertificateId() + "-intermediate-" + cert.getSerialNumber(),
                        cert
                    );
                }
                sinkTruststore.tryEmitNext(trustStore);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to populate PKCS12 KeyStore", e);
        }
    }

    /**
     * Returns a Publisher of the current PKCS12 KeyStore built from the received OCI certificate.
     *
     * If no certificate has been received yet, returns an empty Publisher.
     *
     * @return A Publisher emitting the KeyStore when available, otherwise empty.
     */
    @Override
    public @NonNull Publisher<@NonNull KeyStore> getKeyStore() {
        return bundleKeystore;
    }

    /**
     * Returns a Publisher of the current PKCS12 TrustStore containing the leaf and intermediate certificates.
     *
     * If no certificate has been received yet, returns an empty Publisher.
     *
     * @return A Publisher emitting the TrustStore when available, otherwise empty.
     */
    @Override
    public @NonNull Publisher<@NonNull KeyStore> getTrustStore() {
        return bundleTruststore;
    }

    @Override
    public @NonNull String getName() {
        return configuration.getName();
    }
}
