/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.oraclecloud.certificates.config;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration;

/**
 * Configuration entry for an Oracle Cloud Infrastructure (OCI) certificate.
 * <p>
 * Each instance represents one certificate to be fetched from OCI Certificates service and exposed
 * to Micronaut's SSL infrastructure.
 * </p>
 *
 * @param certificateId The OCID of the certificate.
 * @param versionNumber The specific certificate version number to use, or null to resolve by name/latest.
 * @param certificateVersionName The named certificate version to use, or null.
 * @param enabled Whether this certificate entry is enabled. Defaults to true when unspecified.
 */
@EachProperty(value = OracleCloudCertificateProperties.PREFIX, primary = "default")
@Requires(missingProperty = OracleCloudCertificationsConfiguration.CERTIFICATE_ID)
@BootstrapContextCompatible
public record OracleCloudCertificateConfiguration(
    @NonNull String certificateId,
    @Nullable Long versionNumber,
    @Nullable String certificateVersionName,
    @Nullable Boolean enabled
) implements OracleCloudCertificateProperties {

    /**
     * Whether this certificate entry is enabled.
     *
     * If {@code enabled} is null, this entry defaults to enabled.
     *
     * @return true if this entry is enabled
     */
    @Override
    public boolean isEnabled() {
        // Default to enabled when the global feature flag is on and per-item flag is not set
        return enabled != null ? enabled : true;
    }
}
