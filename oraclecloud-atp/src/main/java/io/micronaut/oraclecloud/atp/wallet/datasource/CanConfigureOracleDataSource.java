/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.oraclecloud.atp.wallet.datasource;

import io.micronaut.core.annotation.Internal;
import oracle.jdbc.datasource.OracleCommonDataSource;

import java.io.IOException;
import java.sql.SQLException;

/**
 * A trait interface for types that can contribute to the configuration of an {@link
 * OracleDataSourceAttributes} instance.
 */
@Internal
public interface CanConfigureOracleDataSource {

    /**
     * Convenience method to configure an {@link OracleCommonDataSource} sub-type.
     *
     * @param ods The data source to be configure
     * @throws SQLException if a database error occurs configuring the data source
     * @throws IOException if an error occurs reading the state embodied in this instance
     */
    default void configure(final OracleCommonDataSource ods) throws SQLException, IOException {
        this.configure(new OracleDataSourceAttributesBase.Configurator<>(ods)).configure();
    }

    /**
     * Configure a data source using the state in this instance.
     *
     * @param dataSource The data source to be configured
     * @param <T> The data source type
     * @return The configured instance
     * @throws SQLException if a database error occurs
     * @throws IOException if an error occurs reading the state embodied in this instance
     */
    <T extends OracleDataSourceAttributes> T configure(T dataSource)
            throws SQLException, IOException;
}
