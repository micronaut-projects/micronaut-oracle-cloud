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

import org.jspecify.annotations.NonNull;
import io.micronaut.core.naming.Named;
import io.micronaut.core.util.Toggleable;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;
import jakarta.annotation.Nullable;

/**
 * Interface for configuration properties for certificates.
 */
public interface OracleCloudCertificateProperties extends Toggleable, Named {
    String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".certificates";

    /**
     * @return Is the certificate enabled.
     */
    boolean enabled();

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
        return enabled();
    }
}
