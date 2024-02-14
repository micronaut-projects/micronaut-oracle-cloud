package io.micronaut.oraclecloud.core;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.Toggleable;

import static io.micronaut.oraclecloud.core.OracleCloudConfigFileConfigurationProperties.PREFIX;

/**
 * Configuration properties for the local OCI config file (eg: {@code $USE_HOME/.oci/config}).
 *
 * @param profile The profile to use.
 * @param configPath A custom path for the OCI configuration file.
 * @param enabled Whether to enable or disable using the OCI configuration file.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 3.6.0
 */
@ConfigurationProperties(PREFIX)
public record OracleCloudConfigFileConfigurationProperties(@Nullable String profile, @Nullable String configPath, @Nullable Boolean enabled) implements Toggleable {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".config";

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
}
