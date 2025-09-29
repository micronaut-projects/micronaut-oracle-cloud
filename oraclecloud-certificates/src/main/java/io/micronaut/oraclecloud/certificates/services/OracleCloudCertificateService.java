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
package io.micronaut.oraclecloud.certificates.services;

import com.oracle.bmc.certificates.Certificates;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.ssl.OracleCloudCertificateFetcher;
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration;
import io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateConfiguration;
import io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateProperties;
import io.micronaut.oraclecloud.certificates.events.CertificateEvent;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service to contact an Oracle Cloud Certificate service and setup a certificate on a given basis.
 */
@Singleton
@Requires(classes = {Certificates.class})
@Requires(beans = {Certificates.class})
@Internal
public class OracleCloudCertificateService {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudCertificateService.class);

    private final List<OracleCloudCertificateProperties> oracleCloudCertificationsConfigurations;
    private final OracleCloudCertificateFetcher oracleCloudCertificateFetcher;
    private final ApplicationEventPublisher<CertificateEvent> eventPublisher;

    /**
     * Constructs a new Oracle Cloud Certificate service.
     *
     * @param oracleCloudCertificationsConfigurations Oracle Cloud Certificate configurations
     * @param eventPublisher Application Event Publisher
     * @param oracleCloudCertificateFetcher Fetcher used to obtain certificate material from OCI.
     */
    @Inject
    public OracleCloudCertificateService(List<OracleCloudCertificateProperties> oracleCloudCertificationsConfigurations,
                                         ApplicationEventPublisher<CertificateEvent> eventPublisher,
                                         OracleCloudCertificateFetcher oracleCloudCertificateFetcher) {
        this.oracleCloudCertificationsConfigurations = oracleCloudCertificationsConfigurations;
        this.eventPublisher = eventPublisher;
        this.oracleCloudCertificateFetcher = oracleCloudCertificateFetcher;
    }


    /**
     * Returns the certificate event.
     *
     * @return certificate event
     */
    @NonNull
    @Deprecated(forRemoval = true)
    protected Optional<CertificateEvent> getCertificateEvent() {
        return Optional.empty();
    }

    /**
     * Retrieves a list of CertificateEvents for the configured Oracle Cloud certificates.
     *
     * This method iterates over the available Oracle Cloud certificate configurations,
     * retrieves the corresponding certificates, and constructs CertificateEvents.
     *
     * If a CertificateException occurs during the retrieval of a certificate,
     * it is logged as a warning and the certificate is skipped.
     *
     * @return a list of CertificateEvents for the configured Oracle Cloud certificates
     */
    public List<CertificateEvent> getCertificateEvents() {
        return this.oracleCloudCertificationsConfigurations.stream()
            .flatMap(config -> {
                try {

                    String certificateId = config.certificateId();
                    Long versionNumber = config.versionNumber();
                    String certificateVersionName = config.certificateVersionName();

                    CertificateEvent certificateEvent = oracleCloudCertificateFetcher.retrieveCertificate(certificateId, versionNumber, certificateVersionName).orElse(null);
                    return Stream.ofNullable(certificateEvent);

                } catch (CertificateException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Could not create certificate from file: " + e.getMessage(), e);
                    }
                    return Stream.empty();
                }
            }).toList();
    }

    /**
     * Refreshes the configured OCI certificates and publishes {@link io.micronaut.oraclecloud.certificates.events.CertificateEvent}s.
     *
     * The retry behavior is controlled by oci.certificates.refresh.retry.* properties and defaults to
     * 3 attempts with a 1s delay between attempts when not configured.
     */
    @Retryable(
        attempts = "${oci.certificates.refresh.retry.attempts:3}",
        delay = "${oci.certificates.refresh.retry.delay:1s}"
    )
    public void refreshCertificate() {
        List<CertificateEvent> certificateEvents = getCertificateEvents();
        for (CertificateEvent certificateEvent : certificateEvents) {
            eventPublisher.publishEvent(certificateEvent);
        }
    }

    /**
     * Maps the deprecated {@link io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration}
     * into the current {@link io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateConfiguration}.
     *
     * @param oracleCloudCertificationsConfiguration The deprecated configuration.
     * @return An equivalent {@link OracleCloudCertificateProperties} instance in the new format.
     */
    private static OracleCloudCertificateProperties handleDeprecation(OracleCloudCertificationsConfiguration oracleCloudCertificationsConfiguration) {
        LOG.warn("Deprecated OracleCloudCertificateService constructor used");
        return new OracleCloudCertificateConfiguration(oracleCloudCertificationsConfiguration.certificateId(), oracleCloudCertificationsConfiguration.versionNumber(), oracleCloudCertificationsConfiguration.certificateVersionName(), oracleCloudCertificationsConfiguration.enabled());
    }
}
