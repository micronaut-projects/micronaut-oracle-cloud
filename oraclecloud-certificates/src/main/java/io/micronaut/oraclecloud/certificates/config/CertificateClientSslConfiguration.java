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
package io.micronaut.oraclecloud.certificates.config;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.naming.Named;
import io.micronaut.http.ssl.ClientSslConfiguration;

/**
 * Implementation of {@link ClientSslConfiguration} for a specific certificate.
 */
@EachBean(ClientOracleCloudCertificateConfiguration.class)
public class CertificateClientSslConfiguration extends ClientSslConfiguration implements Named {
    private final ClientOracleCloudCertificateConfiguration certificateConfiguration;
    private final String name;

    public CertificateClientSslConfiguration(@Parameter String name, ClientOracleCloudCertificateConfiguration certificateConfiguration) {
        this.name = name;
        this.certificateConfiguration = certificateConfiguration;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * @return The certificate configuration.
     */
    public ClientOracleCloudCertificateConfiguration getCertificateConfiguration() {
        return certificateConfiguration;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }
}
