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

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider.InstancePrincipalsAuthenticationDetailsProviderBuilder;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;

import static io.micronaut.oraclecloud.core.InstancePrincipalConfiguration.PREFIX;

/**
 * Allows configuration of the {@link InstancePrincipalsAuthenticationDetailsProvider}.
 *
 * @author graemerocher
 * @author toddsharp
 * @since 1.0.0
 */
@ConfigurationProperties(PREFIX)
@Requires(property = PREFIX + ".enabled", value = StringUtils.TRUE)
public class InstancePrincipalConfiguration implements Toggleable {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".config.instance-principal";

    private boolean enabled = true;
    private String metadataBaseUrl;

    @ConfigurationBuilder(prefixes = "")
    private final InstancePrincipalsAuthenticationDetailsProviderBuilder builder =
            InstancePrincipalsAuthenticationDetailsProvider.builder();

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
     * Get custom metadata base url.
     *
     * @return metadata base url
     */
    public String getMetadataBaseUrl() {
        return metadataBaseUrl;
    }

    /**
     * Sets custom metadata base url.
     *
     * @param metadataBaseUrl custom metadata url
     */
    public void setMetadataBaseUrl(String metadataBaseUrl) {
        this.metadataBaseUrl = metadataBaseUrl;
    }

    /**
     * @return The builder
     */
    public InstancePrincipalsAuthenticationDetailsProviderBuilder getBuilder() {
        if (metadataBaseUrl != null) {
            builder.metadataBaseUrl(metadataBaseUrl);
        }
        return builder;
    }
}
