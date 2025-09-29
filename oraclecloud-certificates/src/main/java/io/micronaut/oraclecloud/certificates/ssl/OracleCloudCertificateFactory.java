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
package io.micronaut.oraclecloud.certificates.ssl;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.ssl.CertificateProvider;
import io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateProperties;
import io.micronaut.http.ssl.OracleCloudCertificateFetcher;

import java.security.cert.CertificateException;

/**
 * Factory that creates Micronaut SSL CertificateProvider beans backed by
 * Oracle Cloud Infrastructure (OCI) Certificates service.
 * <p>
 * The produced CertificateProvider instances preload any available certificate on startup
 * and listen for subsequent refresh events.
 * </p>
 */
@Factory
@BootstrapContextCompatible
@Internal
public class OracleCloudCertificateFactory {

    /**
     * Creates a CertificateProvider for the given certificate configuration.
     *
     * @param configuration The OCI certificate properties for this provider.
     * @param oracleCloudCertificateFetcher The fetcher used to retrieve certificate material from OCI.
     * @return A CertificateProvider that supplies a KeyStore and TrustStore derived from OCI certificates.
     * @throws CertificateException If preloading an existing certificate fails to parse.
     */
    @EachBean(OracleCloudCertificateProperties.class)
    public CertificateProvider oracleCloudCertificateProvider(OracleCloudCertificateProperties configuration,
                                                              OracleCloudCertificateFetcher oracleCloudCertificateFetcher) throws CertificateException {
        return new OracleCloudCertificateProvider(configuration, oracleCloudCertificateFetcher);
    }
}
