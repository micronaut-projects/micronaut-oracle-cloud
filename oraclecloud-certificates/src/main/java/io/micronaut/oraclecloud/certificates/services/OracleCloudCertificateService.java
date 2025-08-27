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
import com.oracle.bmc.certificates.model.CertificateBundle;
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey;
import com.oracle.bmc.certificates.requests.GetCertificateBundleRequest;
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration;
import io.micronaut.oraclecloud.certificates.config.DefaultServerOracleCloudCertificateConfiguration;
import io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateProperties;
import io.micronaut.oraclecloud.certificates.events.CertificateEvent;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service to contact an Oracle Cloud Certificate service and setup a certificate on a given basis.
 */
@Singleton
@Requires(classes = {Certificates.class})
@Requires(beans = {Certificates.class})
@Requires(property = OracleCloudCertificateProperties.PREFIX + ".enabled", value = "true")
@Internal
public class OracleCloudCertificateService {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudCertificateService.class);
    private static final String X509_CERT = "X.509";

    private final List<OracleCloudCertificateProperties> oracleCloudCertificationsConfigurations;
    private final Certificates certificates;
    private final ApplicationEventPublisher<CertificateEvent> eventPublisher;

    /**
     * Constructs a new Oracle Cloud Certificate service.
     *
     * @param oracleCloudCertificationsConfiguration Oracle Cloud Certificate configuration
     * @param certificates                           Oracle Cloud Certificate client
     * @param eventPublisher                         Application Event Publisher
     */
    @Deprecated(forRemoval = true)
    public OracleCloudCertificateService(OracleCloudCertificationsConfiguration oracleCloudCertificationsConfiguration,
                                         Certificates certificates,
                                         ApplicationEventPublisher<CertificateEvent> eventPublisher) {
       this(List.of(handleDeprecation(oracleCloudCertificationsConfiguration)), certificates, eventPublisher);
    }

    /**
     * Constructs a new Oracle Cloud Certificate service.
     *
     * @param oracleCloudCertificationsConfigurations Oracle Cloud Certificate configurations
     * @param certificates                           Oracle Cloud Certificate client
     * @param eventPublisher                         Application Event Publisher
     */
    @Inject
    public OracleCloudCertificateService(List<OracleCloudCertificateProperties> oracleCloudCertificationsConfigurations,
                                         Certificates certificates,
                                         ApplicationEventPublisher<CertificateEvent> eventPublisher) {
        this.oracleCloudCertificationsConfigurations = oracleCloudCertificationsConfigurations;
        this.certificates = certificates;
        this.eventPublisher = eventPublisher;
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
    protected List<CertificateEvent> getCertificateEvents() {
        return this.oracleCloudCertificationsConfigurations.stream()
            .flatMap(config -> {
                try {

                    String certificateId = config.certificateId();
                    Long versionNumber = config.versionNumber();
                    String certificateVersionName = config.certificateVersionName();

                    CertificateEvent certificateEvent = retrieveCertificate(certificateId, versionNumber, certificateVersionName).orElse(null);
                    return Stream.ofNullable(certificateEvent);

                } catch (CertificateException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Could not create certificate from file: " + e.getMessage(), e);
                    }
                    return Stream.empty();
                }
            }).toList();
    }

    private Optional<CertificateEvent> retrieveCertificate(String certificateId, Long versionNumber, String certificateVersionName) throws CertificateException {
        GetCertificateBundleResponse certificateBundle = certificates.getCertificateBundle(GetCertificateBundleRequest.builder()
            .certificateId(certificateId)
            .versionNumber(versionNumber)
            .certificateVersionName(certificateVersionName)
            .certificateBundleType(GetCertificateBundleRequest.CertificateBundleType.CertificateContentWithPrivateKey)
            .build());

        CertificateFactory cf = CertificateFactory.getInstance(X509_CERT);
        List<X509Certificate> intermediate = Collections.emptyList();

        CertificateBundle cb = certificateBundle.getCertificateBundle();
        if (cb.getCertChainPem() != null) {
            intermediate = cf.generateCertificates(
                    new ByteArrayInputStream(
                        cb
                            .getCertChainPem()
                            .getBytes())).stream().map(cert -> ((X509Certificate) cert))
                .collect(Collectors.toList());
        }

        CertificateEvent certificateEvent = new CertificateEvent(
            getPrivateKey(certificateBundle),
            (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cb.getCertificatePem().getBytes())),
            intermediate,
            cb
        );

        return Optional.of(certificateEvent);
    }

    /**
     * Extracts private key from GetCertificateBundleResponse.
     * @param getCertificateBundleResponse response from OCI service
     * @return private key
     */
    private PrivateKey getPrivateKey(GetCertificateBundleResponse getCertificateBundleResponse) {
        try {
            CertificateBundleWithPrivateKey certificateBundleWithPrivateKey =
                (CertificateBundleWithPrivateKey) getCertificateBundleResponse.getCertificateBundle();
            return parsePrivateKey(certificateBundleWithPrivateKey.getPrivateKeyPem());
        } catch (IOException ioException) {
            return null;
        }
    }

    /**
     * Setup the certificate for HTTPS.
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
     * Extracts private key from a PEM String.
     * @param privateKeyPem private key in PEM format
     * @return {@link PrivateKey} private key from PEM format.
     */
    private PrivateKey parsePrivateKey(String privateKeyPem) throws IOException {
        PrivateKeyInfo privateKeyInfo;
        try (var parser = new PEMParser(new StringReader(privateKeyPem))) {
            Object parsedObject = parser.readObject();
            if (parsedObject instanceof PEMKeyPair pemkeypair) {
                privateKeyInfo = pemkeypair.getPrivateKeyInfo();
            } else if (parsedObject instanceof PrivateKeyInfo privateKeyInfoParsed) {
                privateKeyInfo = privateKeyInfoParsed;
            } else {
                throw new IllegalStateException("Unexpected value: " + parser.readObject());
            }
            return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
        } catch (PEMException ex) {
            throw new IOException("Invalid PEM file", ex);
        }
    }

    private static DefaultServerOracleCloudCertificateConfiguration handleDeprecation(OracleCloudCertificationsConfiguration oracleCloudCertificationsConfiguration) {
        LOG.warn("Deprecated OracleCloudCertificateService constructor used");
        return new DefaultServerOracleCloudCertificateConfiguration(oracleCloudCertificationsConfiguration.certificateId(), oracleCloudCertificationsConfiguration.versionNumber(), oracleCloudCertificationsConfiguration.certificateVersionName(), oracleCloudCertificationsConfiguration.enabled());
    }
}
