/*
 * Copyright 2017-2023 original authors
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
        new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder();

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
