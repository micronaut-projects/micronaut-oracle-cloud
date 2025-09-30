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

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.Toggleable;
import io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateProperties;
import jakarta.annotation.Nullable;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.micronaut.oraclecloud.certificates.config.OracleCloudCertificateProperties.PREFIX;

/**
 * Allows the configuration of the default Oracle Cloud certificate to use.
 * @param certificateId ocid of certificate
 * @param versionNumber version number of certificate
 * @param certificateVersionName certificate name
 * @param enabled flag for enabling feature
 */
@ConfigurationProperties(PREFIX)
@Deprecated(forRemoval = true)
@Requires(property = OracleCloudCertificationsConfiguration.CERTIFICATE_ID)
@Named("deprecated")
@BootstrapContextCompatible
public record OracleCloudCertificationsConfiguration(
    @NonNull String certificateId,
    @Nullable Long versionNumber,
    @Nullable String certificateVersionName,
    boolean enabled) implements Toggleable, OracleCloudCertificateProperties {
    public static final String CERTIFICATE_ID = PREFIX + ".certificate-id";
    public static final String ENABLED_PROPERTY = PREFIX + ".enabled";
    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudCertificationsConfiguration.class);

    public OracleCloudCertificationsConfiguration {
        LOG.warn("Configuring server certificate via " + CERTIFICATE_ID + " is deprecated. Use " + OracleCloudCertificateProperties.PREFIX + " namespace instead");
    }

    @Override
    public @NonNull String getName() {
        return "deprecated";
    }
}
