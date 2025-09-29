/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.http.ssl;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.certificates.Certificates;
import com.oracle.bmc.certificates.CertificatesClient;
import com.oracle.bmc.certificates.model.CertificateBundle;
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey;
import com.oracle.bmc.certificates.requests.GetCertificateBundleRequest;
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse;
import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.core.annotation.Internal;
import io.micronaut.oraclecloud.certificates.events.CertificateEvent;
import jakarta.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fetches certificates and private keys from Oracle Cloud Infrastructure (OCI) Certificates service
 * and converts them into {@link io.micronaut.oraclecloud.certificates.events.CertificateEvent} instances
 * consumable by Micronaut SSL infrastructure.
 * <p>
 * This component is intended for internal use and is compatible with the bootstrap context.
 * </p>
 */
@Internal
@Singleton
@BootstrapContextCompatible
public class OracleCloudCertificateFetcher {

    private static final String X509_CERT = "X.509";
    private final Certificates certificates;

    /**
     * Creates a new fetcher that uses the provided OCI Certificates client builder and authentication provider.
     *
     * @param certificatesClientBuilder The OCI Certificates client builder.
     * @param authenticationDetailsProvider The OCI authentication provider used to authorize requests.
     */
    public OracleCloudCertificateFetcher(CertificatesClient.Builder certificatesClientBuilder, AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
        certificatesClientBuilder.httpProvider(HttpProvider.getDefault());
        this.certificates = certificatesClientBuilder.build(authenticationDetailsProvider);
    }

    /**
     * Retrieves a certificate bundle (including private key) from OCI and converts it into a {@link CertificateEvent}.
     *
     * @param certificateId The OCID of the certificate to retrieve. Required.
     * @param versionNumber The specific version number of the certificate to retrieve. Optional; may be {@code null}.
     * @param certificateVersionName The named version of the certificate to retrieve. Optional; may be {@code null}.
     * @return An {@link Optional} containing the resulting {@link CertificateEvent} if retrieval succeeds; otherwise empty.
     * @throws CertificateException If the certificate content cannot be parsed.
     */
    public Optional<CertificateEvent> retrieveCertificate(String certificateId, Long versionNumber, String certificateVersionName) throws CertificateException {
        GetCertificateBundleResponse certificateBundle = certificates.getCertificateBundle(GetCertificateBundleRequest.builder()
            .certificateId(certificateId)
            .versionNumber(versionNumber)
            .certificateVersionName(certificateVersionName)
            .certificateBundleType(GetCertificateBundleRequest.CertificateBundleType.CertificateContentWithPrivateKey)
            .build());

        return getEventFromGetCertificateBundleResponse(certificateBundle);
    }

    /**
     * Converts an OCI {@link GetCertificateBundleResponse} into a {@link CertificateEvent}.
     *
     * @param certificateBundle The response returned by OCI Certificates service.
     * @return An {@link Optional} containing the constructed {@link CertificateEvent}.
     * @throws CertificateException If the certificate chain cannot be parsed.
     */
    static Optional<CertificateEvent> getEventFromGetCertificateBundleResponse(GetCertificateBundleResponse certificateBundle) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance(X509_CERT);
        List<X509Certificate> intermediate = Collections.emptyList();

        CertificateBundle cb = certificateBundle.getCertificateBundle();
        if (cb.getCertChainPem() != null) {
            intermediate = cf.generateCertificates(
                    new ByteArrayInputStream(
                        cb.getCertChainPem().getBytes())).stream().map(cert -> ((X509Certificate) cert))
                .toList();
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
    private static PrivateKey getPrivateKey(GetCertificateBundleResponse getCertificateBundleResponse) {
        try {
            CertificateBundleWithPrivateKey certificateBundleWithPrivateKey =
                (CertificateBundleWithPrivateKey) getCertificateBundleResponse.getCertificateBundle();
            return parsePrivateKey(certificateBundleWithPrivateKey.getPrivateKeyPem(), certificateBundleWithPrivateKey.getPrivateKeyPemPassphrase());
        } catch (IOException | PemParser.NotPemException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts a private key from a PEM-encoded string, optionally protected with a passphrase.
     *
     * @param privateKeyPem The private key in PEM format.
     * @param password The passphrase used to decrypt the private key, or {@code null} if unencrypted.
     * @return The parsed {@link PrivateKey} instance.
     * @throws IOException If the PEM content cannot be read.
     * @throws PemParser.NotPemException If the content is not valid PEM.
     * @throws GeneralSecurityException If the key cannot be parsed or decrypted.
     */
     static PrivateKey parsePrivateKey(String privateKeyPem, String password) throws IOException, PemParser.NotPemException, GeneralSecurityException {
        PemParser pemParser = new PemParser(null, password);
         List<Object> mainObjects = pemParser.loadPem(privateKeyPem);
         if (mainObjects.get(0) instanceof PrivateKey pk) {
             return pk;
         } else {
             throw new IllegalArgumentException("Unexpected PrivateKey: " + mainObjects.get(0));
         }
    }
}
