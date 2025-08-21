package io.micronaut.oraclecloud.certificates.config;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.Toggleable;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;
import jakarta.annotation.Nullable;

/**
 * Interface for configuration properties for certificates.
 */
public interface OracleCloudCertificateProperties extends Toggleable {
    String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".certificates";

    /**
     * @return Is the certificate enabled.
     */
    @Nullable
    Boolean enabled();

    /**
     * @return The ID of the certificate
     */
    @NonNull
    String certificateId();

    /**
     * @return The version number of the certificate
     */
    @Nullable
    Long versionNumber();

    /**
     * @return The version name of the certificate.
     */
    @Nullable String certificateVersionName();

    @Override
    default boolean isEnabled() {
        Boolean e = enabled();
        return e != null && e;
    }
}
