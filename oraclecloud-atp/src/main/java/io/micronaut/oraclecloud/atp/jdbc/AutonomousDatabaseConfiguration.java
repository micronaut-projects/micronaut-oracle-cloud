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
import io.micronaut.context.annotation.Parameter;
import io.micronaut.jdbc.BasicJdbcConfiguration;

import javax.validation.constraints.NotBlank;

/**
 * Configuration properties for the automated oracle wallet download and configuration.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
@Context
public class AutonomousDatabaseConfiguration {

    String name;

    @NotBlank
    String ocid;

    @NotBlank
    String walletPassword;

    GenerateAutonomousDatabaseWalletDetails.GenerateType walletType;

    @NotBlank
    String serviceAlias;

    public AutonomousDatabaseConfiguration(@Parameter String name) {
        this.name = name;
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
     *
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
     *
     * @return A user provided name to identify the datasource
     */
    public String getName() {
        return name;
    }

    /**
     * @param name A user provided name to identify the datasource
     */
    public void setName(String name) {
        this.name = name;
    }
}
