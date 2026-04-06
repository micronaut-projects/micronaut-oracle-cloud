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
import io.micronaut.core.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalized importer declaration configuration for Oracle Cloud Vault.
 *
 * @param compartmentId The Vault compartment identifier
 * @param vaultOcid The Vault OCID
 * @param includes Include patterns applied to secret names
 * @param excludes Exclude patterns applied to secret names
 * @param retryAttempts Retry attempts for secret retrieval
 * @param retryDelay Retry delay string supplied by the importer declaration
 * @param authProperties Normalized OCI authentication-related properties
 * @author graemerocher
 * @since 6.0.0
 */
@Internal
record OracleCloudVaultImportConfiguration(String compartmentId,
                                           String vaultOcid,
                                           List<String> includes,
                                           List<String> excludes,
                                           Integer retryAttempts,
                                           String retryDelay,
                                           Map<String, Object> authProperties) {

    static OracleCloudVaultImportConfiguration of(String compartmentId,
                                                  String vaultOcid,
                                                  List<String> includes,
                                                  List<String> excludes,
                                                  Integer retryAttempts,
                                                  String retryDelay,
                                                  Map<String, Object> authProperties) {

        return new OracleCloudVaultImportConfiguration(compartmentId, vaultOcid, includes, excludes, retryAttempts, retryDelay, authProperties);
    }

    public Map<String, Object> properties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("micronaut.config-client.enabled", true);
        properties.put("oci.vault.config.enabled", true);
        properties.put("micronaut.metrics.export.oraclecloud.enabled", false);
        properties.put("oci.vault.vaults[0].ocid", vaultOcid);
        properties.put("oci.vault.vaults[0].compartment-ocid", compartmentId);
        for (int i = 0; i < includes.size(); i++) {
            properties.put("oci.vault.vaults[0].includes[" + i + ']', includes.get(i));
        }
        for (int i = 0; i < excludes.size(); i++) {
            properties.put("oci.vault.vaults[0].excludes[" + i + ']', excludes.get(i));
        }
        if (retryAttempts != null) {
            properties.put("oci.vault.config.retry-attempts", retryAttempts);
        }
        if (retryDelay != null) {
            properties.put("oci.vault.config.retry-delay", retryDelay);
        }
        properties.putAll(authProperties);
        if (!authProperties.containsKey("oci.config.path") && !authProperties.containsKey("oci.config.profile")) {
            properties.put("oci.config.enabled", StringUtils.FALSE);
        }
        return properties;
    }
}
