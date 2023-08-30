package io.micronaut.oraclecloud.core;

import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;


@ConfigurationProperties(OkeWorkloadIdentityConfiguration.PREFIX)
@BootstrapContextCompatible
@Requires(property = OkeWorkloadIdentityConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE)
public class OkeWorkloadIdentityConfiguration implements Toggleable {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".config.oke-workload-identity";

    private boolean enabled = true;

    @ConfigurationBuilder(prefixes = "")
    private final OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder builder =
        OkeWorkloadIdentityAuthenticationDetailsProvider.builder();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled Sets whether to enable instance principal authentication
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return The builder
     */
    public OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder getBuilder() {
        return builder;
    }
}
