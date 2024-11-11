/*
 * Copyright 2017-2020 original authors
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

import io.micronaut.core.util.ArrayUtils;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.config.ConfigDiscoveryConfiguration;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;

/**
 * OracleCloudVault Client.
 *
 * @author toddsharp
 * @since 1.4.0
 */
@ConfigurationProperties(OracleCloudVaultConfiguration.PREFIX)
@BootstrapContextCompatible
public class OracleCloudVaultConfiguration {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".vault";

    private final OracleCloudVaultClientDiscoveryConfiguration oracleCloudVaultClientDiscoveryConfiguration;

    private List<OracleCloudVault> vaults = Collections.emptyList();

    /**
     * Construct a vault configuration with the given discovery configuration.
     *
     * @param discoveryConfiguration The discovery configuration.
     */
    @Inject
    public OracleCloudVaultConfiguration(OracleCloudVaultClientDiscoveryConfiguration discoveryConfiguration) {
        this.oracleCloudVaultClientDiscoveryConfiguration = discoveryConfiguration;
    }

    /**
     * Construct a default vault configuration.
     */
    public OracleCloudVaultConfiguration() {
        this.oracleCloudVaultClientDiscoveryConfiguration = new OracleCloudVaultClientDiscoveryConfiguration();
    }

    /**
     * @return The discovery service configuration
     */
    public OracleCloudVaultClientDiscoveryConfiguration getDiscoveryConfiguration() {
        return oracleCloudVaultClientDiscoveryConfiguration;
    }

    /**
     * A list of {@link OracleCloudVault} objects that contain secrets that will be retrieved, decoded and set into your application as config variables.
     *
     * @return A list of Vaults to retrieve
     */
    public List<OracleCloudVault> getVaults() {
        return vaults;
    }

    /**
     * A list of {@link OracleCloudVault} objects that contain secrets that will be retrieved, decoded and set into your application as config variables.
     *
     * @param vaults A list of Vaults
     */
    public void setVaults(List<OracleCloudVault> vaults) {
        if (vaults != null) {
            this.vaults = vaults;
        }
    }

    /**
     * An Oracle Cloud Vault.
     */
    @EachProperty(value = "vaults", list = true)
    @BootstrapContextCompatible
    public static class OracleCloudVault {
        private String ocid;
        private String compartmentOcid;
        private String[] includes = StringUtils.EMPTY_STRING_ARRAY;
        private String[] excludes = StringUtils.EMPTY_STRING_ARRAY;

        /**
         * The OCID of the vault that contains secrets that will be retrieved, decoded and set as config vars.
         *
         * @return The OCID of the vault.
         */
        public String getOcid() {
            return ocid;
        }

        /**
         * Sets the OCID of the vault that contains secrets that will be retrieved, decoded and set as config vars.
         *
         * @param ocid the ocid of the vault
         */
        public void setOcid(String ocid) {
            this.ocid = ocid;
        }

        /**
         * The compartment OCID where the vault resides.
         *
         * @return The compartment OCID.
         */
        public String getCompartmentOcid() {
            return compartmentOcid;
        }

        /**
         * Sets the compartment OCID where the vault resides.
         *
         * @param compartmentOcid The compartment OCID
         */
        public void setCompartmentOcid(String compartmentOcid) {
            this.compartmentOcid = compartmentOcid;
        }

        /**
         * Gets the includes array.
         *
         * @return the includes array
         */
        @NonNull
        public String[] getIncludes() {
            return includes;
        }

        /**
         * Sets the includes array of regex patterns to match on secret names. Secrets that match these
         * patterns will be included, except those which also match an exclude pattern.
         * If not provided, then all secrets are included.
         *
         * @param includes the includes array
         */
        public void setIncludes(@NonNull String[] includes) {
            if (ArrayUtils.isNotEmpty(includes)) {
                this.includes = includes;
            }
        }

        /**
         * Gets the excludes array.
         *
         * @return the excludes array
         */
        @NonNull
        public String[] getExcludes() {
            return excludes;
        }

        /**
         * Sets the excludes array of regex patterns to match on secret names. Secrets that match these
         * will always be excluded, even if there is a match on include pattern.
         * If not provided, then no secrets are explicitly excluded.
         *
         * @param excludes the excludes array
         */
        public void setExcludes(@NonNull String[] excludes) {
            if (ArrayUtils.isNotEmpty(excludes)) {
                this.excludes = excludes;
            }
        }

        @Override
        public String toString() {
            return "OracleCloudVault{" +
                "ocid='" + ocid + '\'' +
                ", compartmentOcid='" + compartmentOcid + '\'' +
                ", includes=" + Arrays.toString(includes) +
                ", excludes=" + Arrays.toString(excludes) +
                '}';
        }
    }

    /**
     * The Discovery Configuration class for Oracle Cloud Vault.
     */
    @ConfigurationProperties(ConfigDiscoveryConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class OracleCloudVaultClientDiscoveryConfiguration extends ConfigDiscoveryConfiguration {
        public static final String PREFIX = OracleCloudVaultConfiguration.PREFIX + "." + ConfigDiscoveryConfiguration.PREFIX;

        private int retryAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(1);

        /**
         * Returns the number of attempts to retry when retrieving a secret from the Oracle Cloud Vault.
         *
         * @return the number of retry attempts
         */
        public int getRetryAttempts() {
            return retryAttempts;
        }

        /**
         * Sets the number of attempts to retry when retrieving a secret from the Oracle Cloud Vault.
         *
         * @param retryAttempts the number of retry attempts
         */
        public void setRetryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
        }

        /**
         * Returns the delay between retries retrieving a secret from the Oracle Cloud Vault.
         *
         * @return the duration of time to wait before attempting another connection
         */
        @NonNull
        public Duration getRetryDelay() {
            return retryDelay;
        }

        /**
         * Sets the delay between retries retrieving a secret from the Oracle Cloud Vault.
         * <p>
         * This value determines how long the system waits before attempting another connection after a failed attempt.
         *
         * @param retryDelay the duration of time to wait before attempting another connection
         */
        public void setRetryDelay(@NonNull Duration retryDelay) {
            this.retryDelay = retryDelay;
        }
    }
}
