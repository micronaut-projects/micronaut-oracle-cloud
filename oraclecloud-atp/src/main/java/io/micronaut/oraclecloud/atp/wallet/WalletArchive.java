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
package io.micronaut.oraclecloud.atp.wallet;


import io.micronaut.oraclecloud.atp.wallet.datasource.CanConfigureOracleDataSource;
import io.micronaut.oraclecloud.atp.wallet.datasource.OracleDataSourceAttributes;

import java.io.IOException;
import java.util.Objects;

/**
 * Represents a Zip archive containing at least a {@code cwallet.sso} or {@code ewallet.p12} and
 * optionally a {@code tnsnames.ora}.
 */
public class WalletArchive implements CanConfigureOracleDataSource {
    private final Wallet wallet;
    private final TNSNames tnsNames;

    WalletArchive(Wallet wallet, TNSNames tnsNames) {
        Objects.requireNonNull(wallet);
        this.wallet = wallet;
        this.tnsNames = tnsNames;
    }

    /**
     * Indicates the database service alias (AKA TNS Alias) associated with this io.micronaut.oraclecloud.adb.wallet archive.
     * Since a io.micronaut.oraclecloud.adb.wallet may hold multiple sets of database credentials, this value identifies which
     * service will be used to configure a data source via {@link
     * #configure(OracleDataSourceAttributes)}
     *
     * @return The service alias associated with this io.micronaut.oraclecloud.adb.wallet archive, or null if this io.micronaut.oraclecloud.adb.wallet archive
     *     is not associated with a particular service
     * @see #with(String)
     */
    public String serviceAlias() {
        return wallet.serviceAlias();
    }

    /**
     * Specialize this io.micronaut.oraclecloud.adb.wallet archive to configure the specified service alias (AKA TNS Alias). If
     * the archive contains a {@code tnsnames.ora} and the connection descriptor for the service
     * alias is used to configure the JDBC url of the {@link OracleDataSourceAttributes}
     *
     * @param serviceAlias The alias of the database service to configure
     * @return WalletArchive instance that will additionally configure the user credentials and URL
     *     for the database
     * @see #serviceAlias()
     */
    public WalletArchive with(final String serviceAlias) {
        return new WalletArchive(wallet.with(serviceAlias), tnsNames);
    }

    /**
     * Configure a data source with the state held in this io.micronaut.oraclecloud.adb.wallet archive. If a {@link
     * #serviceAlias()} has been identified then any database credentials in the io.micronaut.oraclecloud.adb.wallet will be used
     * to configure the data source and the corresponding connection descriptor in {@code
     * tnsnames.ora} will be used to configure the JDBC URL.
     *
     * <p>If the io.micronaut.oraclecloud.adb.wallet contains a certificate key store and/or trust store these will be used to
     * configure a TLS connection for the data source.
     *
     * @param dataSource The data source to be configured by this io.micronaut.oraclecloud.adb.wallet
     * @param <T> the data source generic type
     * @return The supplied data source
     * @throws WalletException if an error occurs accessing the io.micronaut.oraclecloud.adb.wallet
     */
    public <T extends OracleDataSourceAttributes> T configure(final T dataSource)
            throws IOException {

        wallet.configure(dataSource);

        if (tnsNames != null) {
            final String serviceAlias = wallet.serviceAlias();
            if (serviceAlias != null) {
                ConnectionDescriptor connectionDescriptor =
                        tnsNames.connectionDescriptor(serviceAlias);
                if (connectionDescriptor != null) {
                    connectionDescriptor.configure(dataSource);
                }
            }
        }

        return dataSource;
    }
}
