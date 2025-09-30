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
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.oraclecloud.core.ServiceOracleCloudClientConfigurationProperties;

import static io.micronaut.oraclecloud.httpclient.netty.NettyClientProperties.SERVICE_ID;

/**
 * Provides the service id for each oci client.
 *
 * @since 5.3.0
 */
@Factory
@Requires(classes = ServiceOracleCloudClientConfigurationProperties.class)
@BootstrapContextCompatible
final class ServiceIdClientFactory {

    @BootstrapContextCompatible
    @EachBean(ServiceOracleCloudClientConfigurationProperties.class)
    ClientConfigurator configurationPerClientBuilder(ServiceOracleCloudClientConfigurationProperties props) {
        return builder -> builder.property(SERVICE_ID, props.getName());
    }
}
