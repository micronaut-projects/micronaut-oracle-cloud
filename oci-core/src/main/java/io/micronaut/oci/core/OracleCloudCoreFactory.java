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
