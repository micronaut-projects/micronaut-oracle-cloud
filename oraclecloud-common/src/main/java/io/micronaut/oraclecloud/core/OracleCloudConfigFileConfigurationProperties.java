/*
 * Copyright 2017-2024 original authors
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
