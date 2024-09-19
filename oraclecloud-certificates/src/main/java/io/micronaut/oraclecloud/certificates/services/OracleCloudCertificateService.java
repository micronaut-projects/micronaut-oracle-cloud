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
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey;
import com.oracle.bmc.certificates.requests.GetCertificateBundleRequest;
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration;
import io.micronaut.oraclecloud.certificates.events.CertificateEvent;
import io.micronaut.retry.annotation.Retryable;
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

/**
 * Service to contact an Oracle Cloud Certificate service and setup a certificate on a given basis.
 */
@Singleton
@Requires(classes = {Certificates.class})
@Requires(beans = {Certificates.class})
@Requires(property = OracleCloudCertificationsConfiguration.PREFIX + ".enabled", value = "true")
public class OracleCloudCertificateService {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudCertificateService.class);
    private static final String X509_CERT = "X.509";

    private final OracleCloudCertificationsConfiguration oracleCloudCertificationsConfiguration;
    private final Certificates certificates;
    private final ApplicationEventPublisher<CertificateEvent> eventPublisher;

    /**
     * Constructs a new Oracle Cloud Certificate service.
     *
     * @param oracleCloudCertificationsConfiguration Oracle Cloud Certificate configuration
     * @param certificates                           Oracle Cloud Certificate client
     * @param eventPublisher                         Application Event Publisher
     */
    public OracleCloudCertificateService(OracleCloudCertificationsConfiguration oracleCloudCertificationsConfiguration,
                                         Certificates certificates,
                                         ApplicationEventPublisher<CertificateEvent> eventPublisher) {
        this.oracleCloudCertificationsConfiguration = oracleCloudCertificationsConfiguration;
        this.certificates = certificates;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Returns the certificate event.
     *
     * @return certificate event
     */
    @NonNull
    protected Optional<CertificateEvent> getCertificateEvent() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance(X509_CERT);

            GetCertificateBundleResponse certificateBundle = certificates.getCertificateBundle(GetCertificateBundleRequest.builder()
                .certificateId(oracleCloudCertificationsConfiguration.certificateId())
                .versionNumber(oracleCloudCertificationsConfiguration.versionNumber())
                .certificateVersionName(oracleCloudCertificationsConfiguration.certificateVersionName())
                .certificateBundleType(GetCertificateBundleRequest.CertificateBundleType.CertificateContentWithPrivateKey)
                .build());

            List<X509Certificate> intermediate = Collections.emptyList();

            if (certificateBundle.getCertificateBundle().getCertChainPem() != null) {
                intermediate = cf.generateCertificates(
                        new ByteArrayInputStream(
                            certificateBundle
                                .getCertificateBundle()
                                .getCertChainPem()
                                .getBytes())).stream().map(cert -> ((X509Certificate) cert))
                    .collect(Collectors.toList());
            }

            CertificateEvent certificateEvent = new CertificateEvent(
                getPrivateKey(certificateBundle),
                (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateBundle.getCertificateBundle().getCertificatePem().getBytes())),
                intermediate
            );

            return Optional.of(certificateEvent);

        } catch (CertificateException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not create certificate from file", e);
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts private key from GetCertificateBundleResponse.
     * @param getCertificateBundleResponse response from OCI service
     * @return private key
     */
    private PrivateKey getPrivateKey(GetCertificateBundleResponse getCertificateBundleResponse) {
        try {
            CertificateBundleWithPrivateKey certificateBundleWithPrivateKey = (CertificateBundleWithPrivateKey) getCertificateBundleResponse.getCertificateBundle();
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
        Optional<CertificateEvent> certificateEvent = getCertificateEvent();
        if (certificateEvent.isPresent()) {
            eventPublisher.publishEvent(certificateEvent.get());
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error("Oracle Cloud certificate could not be loaded from service.");
            }
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
}
