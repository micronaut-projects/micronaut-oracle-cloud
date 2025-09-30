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

import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;

import java.util.Map;


/**
 * ApplicationContext configurer for the OCI Certificates client.
 * <p>
 * Marks the Micronaut ApplicationContext as a bootstrap context and sets
 * {@code oci.clients.certificates.ssl.enabled=true}. This ensures the per-service
 * client configuration bean {@link io.micronaut.oraclecloud.core.ServiceOracleCloudClientConfigurationProperties}
 * named {@code certificates} is created and applied to the OCI Certificates client so SSL and other
 * client options can be customized under the {@code oci.clients.certificates.*} namespace.
 * </p>
 */
@ContextConfigurer
@Internal
public class CertificateClientContextConfigurer implements ApplicationContextConfigurer {
    /**
     * Configure the context for the OCI Certificates client by enabling the
     * per-service client configuration for the {@code certificates} service.
     * Specifically, this sets {@code oci.clients.certificates.ssl.enabled=true} during bootstrap,
     * which causes Micronaut to bind and use
     * {@link io.micronaut.oraclecloud.core.ServiceOracleCloudClientConfigurationProperties}
     * for the Certificates client.
     *
     * @param builder The application context builder
     */
    @Override
    public void configure(ApplicationContextBuilder builder) {
        System.setProperty(Environment.BOOTSTRAP_CONTEXT_PROPERTY, StringUtils.TRUE);
        builder.properties(Map.of(
            "oci.clients.certificates.ssl.enabled", "true"
        ));
    }
}
