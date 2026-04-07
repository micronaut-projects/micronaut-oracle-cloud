/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.oraclecloud.discovery.vault;

import io.micronaut.core.annotation.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logs one-time deprecation warnings for the legacy Oracle Cloud Vault bootstrap configuration client.
 *
 * @author graemerocher
 * @since 6.0.0
 */
@Internal
final class OracleCloudVaultLegacyDeprecationLogger {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudVaultLegacyDeprecationLogger.class);
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    private OracleCloudVaultLegacyDeprecationLogger() {
    }

    static void warnIfUsed() {
        if (WARNED.compareAndSet(false, true)) {
            LOG.warn("Oracle Cloud Vault bootstrap ConfigurationClient support is deprecated and will be removed in a future release. Use micronaut.config.import[0].provider=oraclecloud-vault with Vault details configured under oci.vault.* instead.");
        }
    }
}
