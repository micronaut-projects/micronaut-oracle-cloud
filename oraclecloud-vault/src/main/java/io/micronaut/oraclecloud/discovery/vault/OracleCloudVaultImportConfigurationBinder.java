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

import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Binds importer declarations to normalized Oracle Cloud Vault properties.
 *
 * @author graemerocher
 * @since 6.0.0
 */
@Internal
final class OracleCloudVaultImportConfigurationBinder {

    private static final String PROVIDER = OracleCloudVaultPropertySourceImporter.PROVIDER;

    OracleCloudVaultImportConfiguration bind(ConnectionString connectionString) {
        validateProtocol(connectionString.getProtocol());
        String compartmentId = resolveCompartmentId(connectionString);
        String vaultOcid = resolveVaultOcid(connectionString);
        if (compartmentId.isBlank() || vaultOcid.isBlank()) {
            throw new ConfigurationException("Config import provider [oraclecloud-vault] requires syntax oraclecloud-vault://<compartment-id>/<vault-ocid>");
        }
        return OracleCloudVaultImportConfiguration.of(
            compartmentId,
            vaultOcid,
            collectRepeatedOption(connectionString, "includes"),
            collectRepeatedOption(connectionString, "excludes"),
            parseIntegerOption(connectionString, "retry-attempts"),
            connectionString.getOptions().get("retry-delay"),
            bindAuth(connectionString.getUsername(), connectionString.getPassword(), connectionString.getOptions())
        );
    }

    OracleCloudVaultImportConfiguration bind(ConvertibleValues<Object> values) {
        String compartmentId = required(values, "compartment-id");
        String vaultOcid = required(values, "ocid");
        return OracleCloudVaultImportConfiguration.of(
            compartmentId,
            vaultOcid,
            list(values, "includes"),
            list(values, "excludes"),
            values.get("retry-attempts", Integer.class).orElse(null),
            values.get("retry-delay", String.class).orElse(null),
            bindAuth(Optional.ofNullable(values.get("username", String.class).orElse(null)), Optional.ofNullable(values.get("password", String.class).orElse(null)), values.asMap())
        );
    }

    private static void validateProtocol(String protocol) {
        if (!PROVIDER.equals(protocol)) {
            throw new ConfigurationException("Config import provider [oraclecloud-vault] received unsupported protocol [" + protocol + "]");
        }
    }

    private static String resolveCompartmentId(ConnectionString connectionString) {
        if (!connectionString.getHosts().isEmpty()) {
            return connectionString.getHosts().get(0).host();
        }
        String path = trimLeadingSlash(connectionString.getPath());
        int slash = path.indexOf('/');
        return slash > -1 ? path.substring(0, slash) : "";
    }

    private static String resolveVaultOcid(ConnectionString connectionString) {
        String path = trimLeadingSlash(connectionString.getPath());
        int slash = path.indexOf('/');
        if (slash > -1) {
            return path.substring(slash + 1);
        }
        return path;
    }

    private static String required(ConvertibleValues<Object> values, String key) {
        return values.get(key, String.class)
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new ConfigurationException("Config import provider [oraclecloud-vault] requires '" + key + "'"));
    }

    private static List<String> list(ConvertibleValues<Object> values, String key) {
        return values.get(key, String[].class)
            .map(List::of)
            .orElseGet(() -> values.get(key, List.class).map(raw -> raw.stream().map(String::valueOf).toList()).orElseGet(List::of));
    }

    private static Integer parseIntegerOption(ConnectionString connectionString, String key) {
        String value = connectionString.getOptions().get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Config import provider [oraclecloud-vault] option '" + key + "' must be an integer", e);
        }
    }

    private static List<String> collectRepeatedOption(ConnectionString connectionString, String key) {
        List<String> values = new ArrayList<>();
        String single = connectionString.getOptions().get(key);
        if (single != null) {
            for (String part : single.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !values.contains(trimmed)) {
                    values.add(trimmed);
                }
            }
        }
        return values;
    }

    private static Map<String, Object> bindAuth(Optional<String> username, Optional<String> password, Map<String, ?> options) {
        Map<String, Object> properties = new LinkedHashMap<>();
        username.ifPresent(value -> properties.put("oci.auth.username", value));
        password.ifPresent(value -> properties.put("oci.auth.password", value));
        copyOption(options, properties, "auth-strategy", "oci.auth.strategy");
        copyOption(options, properties, "config-path", "oci.config.path");
        copyOption(options, properties, "config-profile", "oci.config.profile");
        copyOption(options, properties, "tenant-id", "oci.auth.tenant-id");
        copyOption(options, properties, "user-id", "oci.auth.user-id");
        copyOption(options, properties, "fingerprint", "oci.auth.fingerprint");
        copyOption(options, properties, "private-key", "oci.auth.private-key");
        copyOption(options, properties, "passphrase", "oci.auth.pass-phrase");
        copyOption(options, properties, "session-token", "oci.session-token.token");
        copyOption(options, properties, "region", "oci.region");
        copyOption(options, properties, "use-instance-principal", "oci.config.use-instance-principal");
        copyOption(options, properties, "use-resource-principal", "OCI_RESOURCE_PRINCIPAL_VERSION");
        copyOption(options, properties, "use-oke-workload-identity", "oci.config.oke-workload-identity.enabled");
        return properties;
    }

    private static void copyOption(Map<String, ?> options, Map<String, Object> target, String from, String to) {
        Object value = options.get(from);
        if (value != null) {
            target.put(to, value);
        }
    }

    private static String trimLeadingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
