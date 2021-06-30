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

import java.util.List;

import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.vault.VaultsClient;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Requires;
import io.micronaut.discovery.config.ConfigDiscoveryConfiguration;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;

/**
 *  OracleCloudVault Client.
 *
 *  @author toddsharp
 *  @since 2.0.0
 */
@ConfigurationProperties(OracleCloudVaultClientConfiguration.PREFIX)
@BootstrapContextCompatible
@Requires(property = OracleCloudVaultClientConfiguration.PREFIX)
@Requires(beans = { VaultsClient.class, SecretsClient.class })
public class OracleCloudVaultClientConfiguration {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".vault";

    private final OracleCloudVaultClientDiscoveryConfiguration oracleCloudVaultClientDiscoveryConfiguration = new OracleCloudVaultClientDiscoveryConfiguration();

    private List<OracleCloudVault> vaults;

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
        this.vaults = vaults;
    }

       /**
     * An Oracle Cloud Vault.
     */
    @EachProperty(value = "vaults", list = true)
    @BootstrapContextCompatible
    public static class OracleCloudVault {
        private String ocid;
        private String compartmentOcid;

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

        @Override
        public String toString() {
            return "OracleCloudVault{" +
                    "ocid='" + ocid + '\'' +
                    ", compartmentOcid='" + compartmentOcid + '\'' +
                    '}';
        }
    }

    /**
     * The Discovery Configuration class for Oracle Cloud Vault.
     */
    @ConfigurationProperties(ConfigDiscoveryConfiguration.PREFIX)
    @BootstrapContextCompatible
    public static class OracleCloudVaultClientDiscoveryConfiguration extends ConfigDiscoveryConfiguration {
        public static final String PREFIX = OracleCloudVaultClientConfiguration.PREFIX + "." + ConfigDiscoveryConfiguration.PREFIX;
    }
}
