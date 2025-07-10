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
package io.micronaut.oraclecloud.core;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.naming.Named;
import io.micronaut.http.client.DefaultHttpClientConfiguration;

/**
 * Configuration for each of the OCI SDK clients.
 *
 * @since 5.2.1
 */
@EachProperty(OracleCloudCoreFactory.ORACLE_CLOUD + ".clients")
@BootstrapContextCompatible
public final class ServiceOracleCloudClientConfigurationProperties extends AbstractOracleCloudClientConfigurationProperties implements Named {
    private String serviceId;

    public ServiceOracleCloudClientConfigurationProperties(@Parameter String serviceId, DefaultHttpClientConfiguration defaultHttpClientConfiguration) {
        super(defaultHttpClientConfiguration);
        this.serviceId = serviceId;
    }

    /**
     * @return the service id.
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * @param serviceId the service id.
     */
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * @return the serviceId for a {@link Named} used by {@link jakarta.inject.Named}.
     */
    @Override
    public @NonNull String getName() {
        return serviceId;
    }
}
