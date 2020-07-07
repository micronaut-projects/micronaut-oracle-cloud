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
package io.micronaut.oci.core;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;
import java.io.IOException;

/**
 * Sets up core beans for integration with Oracle cloud clients.
 *
 * @see AuthenticationDetailsProvider
 */
@Factory
public class OracleCloudCoreFactory {

    /**
     * Configures a {@link ConfigFileAuthenticationDetailsProvider} if no other {@link AuthenticationDetailsProvider} is present.
     * @param config The config to use
     * @return The {@link AuthenticationDetailsProvider}.
     * @throws IOException If an exception occurs reading configuration.
     */
    @Singleton
    @Requires(missingBeans = AuthenticationDetailsProvider.class)
    @Primary
    protected AuthenticationDetailsProvider authenticationDetailsProvider(
            OracleCloudConfigurationProperties config) throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(
                config.getProfile().orElse(null)
        );
    }

    /**
     * Produces a {@link com.oracle.bmc.ClientConfiguration.ClientConfigurationBuilder} bean for the given properties.
     * @param props The props
     * @return The builder
     */
    @Singleton
    @Primary
    protected ClientConfiguration.ClientConfigurationBuilder configurationBuilder(
            OracleCloudConfigurationProperties props) {
        return props.getClientBuilder();
    }

    /**
     * Configures the default {@link ClientConfiguration} if no other configuration is present.
     * @param builder The builder
     * @return The default client configuration.
     */
    @Singleton
    @Requires(missingBeans = ClientConfiguration.class)
    @Primary
    protected ClientConfiguration clientConfiguration(ClientConfiguration.ClientConfigurationBuilder builder) {
        return builder.build();
    }
}
