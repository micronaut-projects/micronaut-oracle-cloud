package io.micronaut.oraclecloud.certificates.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.core.annotation.NonNull;
import jakarta.annotation.Nullable;

/**
 * Configuration for named clients.
 *
 * @param certificateId          The certificate ID
 * @param versionNumber          The version number
 * @param certificateVersionName The certificate version name
 * @param enabled                Whether the certificate is enabled.
 */
@EachProperty(value = OracleCloudCertificateProperties.PREFIX + ".clients", primary = "default")
public record ClientOracleCloudCertificateConfiguration(
    @NonNull String certificateId,
    @Nullable Long versionNumber,
    @Nullable String certificateVersionName,
    @Nullable Boolean enabled) implements OracleCloudCertificateProperties {

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled;
    }
}
