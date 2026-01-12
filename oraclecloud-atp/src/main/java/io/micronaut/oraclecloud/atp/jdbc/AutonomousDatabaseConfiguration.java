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
package io.micronaut.oraclecloud.atp.jdbc;

import com.oracle.bmc.database.model.GenerateAutonomousDatabaseWalletDetails;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.jdbc.BasicJdbcConfiguration;

/**
 * Configuration properties for the automated oracle wallet download and configuration.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
@Context
public class AutonomousDatabaseConfiguration {

    private String ocid;

    private String walletPassword;

    private GenerateAutonomousDatabaseWalletDetails.GenerateType walletType;

    private String serviceAlias;

    /**
     * Suffix for the default service alias. If {@link #serviceAlias} is not configured,
     * the default service alias will be computed as (dbName + "_" + serviceAliasSuffix).
     * Defaults to "high".
     */
    private String serviceAliasSuffix = "high";

    /**
     * Authentication mode for connecting to ADB. Defaults to PASSWORD (current behavior).
     * When set to IAM, the JDBC connection will use token-based authentication with a provided token properties provider.
     */
    private AuthMode authMode;

    /**
     * Optional bean qualifier (name) to select a specific IamDbTokenProvider for this datasource.
     * If not set, the primary provider will be used if present.
     */
    private String iamProviderQualifier;

    /**
     * Authentication mode options.
     */
    public enum AuthMode {
        PASSWORD,
        IAM
    }

    /**
     * @return autonomous database ocid
     */
    public String getOcid() {
        return ocid;
    }

    /**
     * @param ocid autonomous database ocid
     */
    public void setOcid(String ocid) {
        this.ocid = ocid;
    }

    /**
     * @return wallet password
     */
    public String getWalletPassword() {
        return walletPassword;
    }

    /**
     * @param walletPassword wallet password
     */
    public void setWalletPassword(String walletPassword) {
        this.walletPassword = walletPassword;
    }

    /**
     * @return wallet type
     */
    public GenerateAutonomousDatabaseWalletDetails.GenerateType getWalletType() {
        return walletType;
    }

    /**
     * @param walletType wallet type
     */
    public void setWalletType(GenerateAutonomousDatabaseWalletDetails.GenerateType walletType) {
        this.walletType = walletType;
    }

    /**
     * @return service alias
     */
    public String getServiceAlias() {
        return serviceAlias;
    }

    /**
     * @param serviceAlias service alias
     */
    public void setServiceAlias(String serviceAlias) {
        this.serviceAlias = serviceAlias;
    }

    /**
     * @return service alias suffix. Defaults to "high" if not set.
     */
    public String getServiceAliasSuffix() {
        return serviceAliasSuffix;
    }

    /**
     * @param serviceAliasSuffix service alias suffix to use when service alias is not configured
     */
    public void setServiceAliasSuffix(String serviceAliasSuffix) {
        this.serviceAliasSuffix = serviceAliasSuffix;
    }

    /**
     * @return Authentication mode. Null means default (PASSWORD).
     */
    public AuthMode getAuthMode() {
        return authMode;
    }

    /**
     * @param authMode Authentication mode to use for ADB connections.
     */
    public void setAuthMode(AuthMode authMode) {
        this.authMode = authMode;
    }

    /**
     * @return Optional qualifier (bean name) of the IAM token provider to use.
     */
    public String getIamProviderQualifier() {
        return iamProviderQualifier;
    }

    /**
     * @param iamProviderQualifier Qualifier (bean name) of the IAM token provider to use.
     */
    public void setIamProviderQualifier(String iamProviderQualifier) {
        this.iamProviderQualifier = iamProviderQualifier;
    }
}
