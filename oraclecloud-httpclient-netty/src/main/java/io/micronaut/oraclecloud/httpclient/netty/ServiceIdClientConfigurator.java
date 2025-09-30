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
package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.client.HttpClientBuilder;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.oraclecloud.core.ServiceOracleCloudClientConfigurationProperties;
import jakarta.inject.Singleton;

import static io.micronaut.oraclecloud.httpclient.netty.NettyClientProperties.SERVICE_ID;

/**
 * ClientConfigurator that writes the Oracle Cloud service identifier into the OCI HTTP client builder.
 * <p>
 * Instances are created per configured service via {@link io.micronaut.context.annotation.EachBean}
 * of {@link io.micronaut.oraclecloud.core.ServiceOracleCloudClientConfigurationProperties}. The configurator
 * sets the {@code SERVICE_ID} property on the underlying Netty client so downstream components can
 * distinguish requests per service (for logging, metrics, connection management, etc.).
 * </p>
 *
 * @since 5.3.0
 */
@Requires(classes = ServiceOracleCloudClientConfigurationProperties.class)
@BootstrapContextCompatible
@EachBean(ServiceOracleCloudClientConfigurationProperties.class)
@Singleton
@Internal
public final class ServiceIdClientConfigurator implements ClientConfigurator {

    private final ServiceOracleCloudClientConfigurationProperties oracleCloudClientConfigurationProperties;

    /**
     * Creates a configurator for a specific Oracle Cloud service.
     *
     * @param oracleCloudClientConfigurationProperties The service-specific configuration whose
     *                                                 {@link io.micronaut.core.naming.Named#getName()} is used as the service identifier.
     */
    public ServiceIdClientConfigurator(ServiceOracleCloudClientConfigurationProperties oracleCloudClientConfigurationProperties) {
        this.oracleCloudClientConfigurationProperties = oracleCloudClientConfigurationProperties;
    }

    /**
     * Sets the service identifier property on the OCI {@link HttpClientBuilder}.
     *
     * @param httpClientBuilder The OCI SDK HTTP client builder to customize.
     */
    @Override
    public void customizeClient(HttpClientBuilder httpClientBuilder) {
        if (httpClientBuilder instanceof NettyHttpClientBuilder) {
            httpClientBuilder.property(SERVICE_ID, oracleCloudClientConfigurationProperties.getName());
        }
    }
}
