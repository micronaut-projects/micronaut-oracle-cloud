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
package io.micronaut.oraclecloud.certificates.deprecated;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.ssl.ServerSslConfiguration;
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration;
import jakarta.inject.Singleton;

/**
 * Deprecated listener that amends the ServerSslConfiguration when the legacy
 * oraclecloud.certificates.certificate-id property is used.
 * <p>
 * If no keyName is set on the server SSL configuration, this listener sets it to "deprecated"
 * so Micronaut selects the deprecated certificate configuration bean.
 * </p>
 */
@Singleton
@Requires(beans = OracleCloudCertificationsConfiguration.class)
@Requires(property = OracleCloudCertificationsConfiguration.CERTIFICATE_ID)
@Deprecated(forRemoval = true)
public class OracleCloudCertificateBeanCreatedEventListener implements BeanCreatedEventListener<ServerSslConfiguration> {
    /**
     * Ensures a keyName is present on the created ServerSslConfiguration when using deprecated configuration.
     *
     * @param event The bean creation event for ServerSslConfiguration
     * @return The possibly modified ServerSslConfiguration
     */
    @Override
    public ServerSslConfiguration onCreated(@NonNull BeanCreatedEvent<ServerSslConfiguration> event) {
        ServerSslConfiguration serverSslConfiguration = event.getBean();

        if (serverSslConfiguration.getKeyName() == null || serverSslConfiguration.getKeyName().isEmpty()) {
            serverSslConfiguration.setKeyName("deprecated");
        }
        return serverSslConfiguration;
    }
}
