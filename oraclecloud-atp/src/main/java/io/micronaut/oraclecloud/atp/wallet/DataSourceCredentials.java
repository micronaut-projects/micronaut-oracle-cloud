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


import io.micronaut.oraclecloud.atp.wallet.datasource.OracleDataSourceAttributes;
import oracle.security.pki.OracleKeyStoreSpi;
import oracle.security.pki.OracleSecretStore;
import oracle.security.pki.OracleSecretStoreException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Holds the database credentials and possibly the URL for the data source */
class DataSourceCredentials {
    private static final String ORACLE_THIN_JDBC_PREFIX = "jdbc:oracle:thin:";
    private static final String PASSWORD = OracleKeyStoreSpi.CREDENTIAL_PASSWORD;
    private static final String SERVICE_NAME_SYNTAX_PREFIX = "@//";
    private static final String USER = OracleKeyStoreSpi.CREDENTIAL_USERNAME;
    private static final String CONNECT_STRING_PREFIX = OracleKeyStoreSpi.CREDENTIAL_CONNECT_STRING;
    private final int index;
    private final OracleSecretStore store;
    private final String url;
    private final String user;

    private DataSourceCredentials(
            final OracleSecretStore store, final int index, final String url, final String user) {
        this.store = store;
        this.index = index;
        this.url = url;
        this.user = user;
    }

    static Map<String, DataSourceCredentials> credentials(final OracleSecretStore store)
            throws WalletException {
        try {
            Map<String, DataSourceCredentials> credentials = new LinkedHashMap<>();
            int i = 1;
            boolean check = true;
            while (check) {
                if (store.containsAlias(CONNECT_STRING_PREFIX + i)) {
                    final String serviceAlias =
                            new String(store.getSecret(CONNECT_STRING_PREFIX + i));
                    final DataSourceCredentials dataSourceCredentials =
                            DataSourceCredentials.of(store, serviceAlias, i);
                    credentials.put(serviceAlias, dataSourceCredentials);
                    ++i;
                } else {
                    check = false;
                }
            }
            return credentials;
        } catch (OracleSecretStoreException e) {
            throw WalletException.of(e);
        }
    }

    private static DataSourceCredentials of(
            final OracleSecretStore store, final String serviceAlias, final int index)
            throws WalletException {
        try {
            String url = null;
            String user = new String(store.getSecret(USER + index));
            if (serviceAlias.startsWith(SERVICE_NAME_SYNTAX_PREFIX)) {

                final StringBuilder text = new StringBuilder();
                text.append(ORACLE_THIN_JDBC_PREFIX);
                text.append(serviceAlias);
                url = text.toString();
            }
            // We do not materialize the password eagerly since it is a sensitive value
            return new DataSourceCredentials(store, index, url, user);
        } catch (OracleSecretStoreException e) {
            throw WalletException.of(e);
        }
    }

    <T extends OracleDataSourceAttributes> T configure(T dataSource) throws WalletException {
        if (url != null) {
            dataSource.url(url);
        }
        dataSource.user(user);
        final char[] password = secret(PASSWORD);
        try {
            dataSource.password(password);
        } finally {
            erase(password);
        }
        return dataSource;
    }

    private void erase(char[] password) {
        if (password != null) {
            for (int i = 0; i < password.length; ++i) {
                password[i] = '*';
            }
        }
    }

    private char[] secret(final String prefix) throws WalletException {
        try {
            return store.getSecret(prefix + index);
        } catch (final OracleSecretStoreException e) {
            throw WalletException.of(e);
        }
    }
}
