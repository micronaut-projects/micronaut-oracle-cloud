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
package io.micronaut.oraclecloud.certificates;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;
import jakarta.annotation.Nullable;

import static io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration.PREFIX;

/**
 * Allows the configuration of the Oracle Cloud certificate process.
 * @param certificateId ocid of certificate
 * @param versionNumber version number of certificate
 * @param certificateVersionName certificate name
 * @param enabled flag for enabling feature
 */
@ConfigurationProperties(PREFIX)
public record OracleCloudCertificationsConfiguration(String certificateId, @Nullable Long versionNumber, @Nullable String certificateVersionName, @Nullable Boolean enabled)  implements Toggleable  {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".certificates";

    /**
     * If Oracle Cloud certificate background and setup process should be enabled.
     *
     * @return True if Oracle Cloud certificate process is enabled.
     */
    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : false;
    }
}
