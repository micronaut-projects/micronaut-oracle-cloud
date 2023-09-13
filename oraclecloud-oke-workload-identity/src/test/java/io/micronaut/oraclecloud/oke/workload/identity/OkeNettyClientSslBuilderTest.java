/*
 * Copyright 2017-2020 original authors
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


import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpVersionSelection;
import io.micronaut.http.ssl.ClientAuthentication;
import io.micronaut.http.ssl.SslConfiguration;
import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManagerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static io.micronaut.oraclecloud.oke.workload.identity.MicronautOkeWorkloadIdentityResourcePrincipalsFederationClientTest.CERTIFICATE_STRING;

final class OkeNettyClientSslBuilderTest  {

    @Test
    public void testInitializationSsContextAuthenticationNeed() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        File file = File.createTempFile("temp", ".ca");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(CERTIFICATE_STRING);
        writer.flush();

        try (InputStream inputStream = new FileInputStream(file)) {
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
            OkeNettyClientSslBuilder okeNettyClientSslBuilder = new OkeNettyClientSslBuilder(tmf, keyStore);
            SslConfiguration sslConfiguration = new SslConfiguration();
            sslConfiguration.setCiphers(new String[] { "TEST" });
            sslConfiguration.setProtocol("TLS");
            sslConfiguration.setClientAuthentication(ClientAuthentication.NEED);
            SslContext sslContext = okeNettyClientSslBuilder.build(sslConfiguration,
                HttpVersionSelection.forClientConfiguration(new DefaultHttpClientConfiguration())
            );
            Assertions.assertTrue(sslContext.isClient());
            Assertions.assertEquals(sslContext.cipherSuites().get(0), "TEST");
        }
    }

    @Test
    public void testInitializationSslContextAuthenticationWant() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        File file = File.createTempFile("temp", ".ca");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(CERTIFICATE_STRING);
        writer.flush();

        try (InputStream inputStream = new FileInputStream(file)) {
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
            OkeNettyClientSslBuilder okeNettyClientSslBuilder = new OkeNettyClientSslBuilder(tmf, keyStore);
            SslConfiguration sslConfiguration = new SslConfiguration();
            sslConfiguration.setCiphers(new String[] { "TEST" });
            sslConfiguration.setProtocol("TLS");
            sslConfiguration.setClientAuthentication(ClientAuthentication.WANT);
            SslContext sslContext = okeNettyClientSslBuilder.build(sslConfiguration,
                HttpVersionSelection.forClientConfiguration(new DefaultHttpClientConfiguration())
            );
            Assertions.assertTrue(sslContext.isClient());
            Assertions.assertEquals(sslContext.cipherSuites().get(0), "TEST");
        }
    }

}
