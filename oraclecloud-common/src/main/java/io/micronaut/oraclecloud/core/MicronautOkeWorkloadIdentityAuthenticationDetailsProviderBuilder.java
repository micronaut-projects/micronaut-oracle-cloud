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
package io.micronaut.oraclecloud.core;

import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.auth.internal.FederationClient;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeTenancyOnlyAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeWorkloadIdentityResourcePrincipalsFederationClient;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.client.StandardClientProperties;

import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/** Builder for OkeWorkloadIdentityAuthenticationDetailsProviderBuilder. */
public class MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder extends OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder {

    private static final String KUBERNETES_SERVICE_ACCOUNT_CERT_PATH =
        "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

    /** The configuration for the circuit breaker. */
    private CircuitBreakerConfiguration circuitBreakerConfig;

     /**
     Sets value for the circuit breaker configuration".
     @param circuitBreakerConfig the CircuitBreakerConfiguration
     */
     @Override
     public OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder circuitBreakerConfig(
        CircuitBreakerConfiguration circuitBreakerConfig) {
        this.circuitBreakerConfig = circuitBreakerConfig;
        return this;
    }

    @Override
    protected FederationClient createFederationClient(SessionKeySupplier sessionKeySupplier) {
        OkeTenancyOnlyAuthenticationDetailsProvider provider =
            new OkeTenancyOnlyAuthenticationDetailsProvider();

        // Set ca cert when talking to proxymux using https.
        if (Files.exists(Paths.get(KUBERNETES_SERVICE_ACCOUNT_CERT_PATH))) {
            InputStream inputStream = null;
            try {
                inputStream =
                    new FileInputStream(
                        Paths.get(KUBERNETES_SERVICE_ACCOUNT_CERT_PATH).toFile());
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
            } catch (CertificateException e) {
                throw new IllegalArgumentException(
                    "Invalid Kubernetes ca certification. Please contact OKE Foundation team for help.",
                    e);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(
                    "Kubernetes service account ca cert doesn't exist.", e);
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
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(
                        "Kubernetes service account ca cert doesn't exist.", e);
                }
            }
        } else {
            throw new IllegalArgumentException(
                "Kubernetes service account ca cert doesn't exist.");
        }

        ClientConfigurator configurator = builder -> {
            builder.property(StandardClientProperties.BUFFER_REQUEST, false);
        };

        List<ClientConfigurator> additionalConfigurators = new ArrayList<>();
        if (this.federationClientConfigurator != null) {
            additionalConfigurators.add(this.federationClientConfigurator);
        }
        additionalConfigurators.addAll(this.additionalFederationClientConfigurators);

        // create federation client
        return new OkeWorkloadIdentityResourcePrincipalsFederationClient(
            sessionKeySupplier,
            provider,
            configurator,
            circuitBreakerConfig,
            additionalConfigurators);
    }

}
