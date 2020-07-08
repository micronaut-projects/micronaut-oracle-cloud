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
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

/**
 * Sets up core beans for integration with Oracle cloud clients. The following beans are exposed:
 *
 * <ul>
 *     <li>{@code AuthenticationDetailsProvider}</li>
 *     <li>{@code ClientConfiguration.ClientConfigurationBuilder}</li>
 *     <li>{@code ClientConfiguration}</li>
 * </ul>
 *
 * @see com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
 * @see ResourcePrincipalAuthenticationDetailsProvider
 * @see SimpleAuthenticationDetailsProvider
 */
@Factory
public class OracleCloudCoreFactory {

    public static final String ORACLE_CLOUD = "oci";

    private final String profile;

    /**
     * @param profile The configured profile
     */
    protected OracleCloudCoreFactory(@Nullable @Property(name = ORACLE_CLOUD + ".config.profile") String profile) {
        this.profile = profile;
    }

    /**
     * Configures a {@link ConfigFileAuthenticationDetailsProvider} if no other {@link com.oracle.bmc.auth.AuthenticationDetailsProvider} is present and
     * a file is found at {@code $USER_HOME/.oci/config} or specified by the user with {@code oci.config}.
     *
     * @return The {@link ConfigFileAuthenticationDetailsProvider}.
     * @throws IOException If an exception occurs reading configuration.
     * @see ConfigFileAuthenticationDetailsProvider
     */
    @Singleton
    @Requires(condition = OracleCloudConfigCondition.class)
    @Requires(missingProperty = OracleCloudAuthConfigurationProperties.TENANT_ID)
    @Primary
    protected ConfigFileAuthenticationDetailsProvider configFileAuthenticationDetailsProvider() throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(
                profile
        );
    }

    /**
     * Configures a {@link SimpleAuthenticationDetailsProvider} if no other {@link com.oracle.bmc.auth.AuthenticationDetailsProvider} is present.
     * @param config The config to use
     * @return The {@link SimpleAuthenticationDetailsProvider}.
     * @see SimpleAuthenticationDetailsProvider
     */
    @Singleton
    @Requires(property = OracleCloudAuthConfigurationProperties.TENANT_ID)
    @Primary
    protected SimpleAuthenticationDetailsProvider simpleAuthenticationDetailsProvider(
            OracleCloudAuthConfigurationProperties config) {
        return config.getBuilder().build();
    }

    /**
     * Configures a {@link com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider} if no other {@link com.oracle.bmc.auth.AuthenticationDetailsProvider} is present and
     * the {@code OCI_RESOURCE_PRINCIPAL_VERSION} variable is available in the environment.
     *
     * @return The {@link ResourcePrincipalAuthenticationDetailsProvider}.
     * @see com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider
     */
    @Singleton
    @Requires(property = "OCI_RESOURCE_PRINCIPAL_VERSION")
    @Primary
    protected ResourcePrincipalAuthenticationDetailsProvider resourcePrincipalAuthenticationDetailsProvider() {
        return ResourcePrincipalAuthenticationDetailsProvider.builder().build();
    }

    /**
     * Produces a {@link com.oracle.bmc.ClientConfiguration.ClientConfigurationBuilder} bean for the given properties.
     * @param props The props
     * @return The builder
     */
    @Singleton
    @Primary
    protected ClientConfiguration.ClientConfigurationBuilder configurationBuilder(
            OracleCloudClientConfigurationProperties props) {
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

    /**
     * @return The configured profile.
     */
    public Optional<String> getProfile() {
        return Optional.ofNullable(profile);
    }
}
