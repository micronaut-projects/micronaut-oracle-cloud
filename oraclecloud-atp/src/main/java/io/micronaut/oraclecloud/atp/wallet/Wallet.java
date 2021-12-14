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
import oracle.security.pki.OracleKeyStoreSpi;
import oracle.security.pki.OraclePKIProvider;
import oracle.security.pki.OracleSecretStore;
import oracle.security.pki.OracleSecretStoreException;
import oracle.security.pki.OracleWallet;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Map;

/** Represents an Oracle {@code cwallet.sso} or {@code ewallet.p12} wallet */
final class Wallet implements CanConfigureOracleDataSource {

    private static final String TLS_PROTOCOL = "TLS";
    private static final String CONNECT_STRING = OracleKeyStoreSpi.CREDENTIAL_CONNECT_STRING;
    private static final String PASSWORD = OracleKeyStoreSpi.CREDENTIAL_PASSWORD;
    private static final String USER = OracleKeyStoreSpi.CREDENTIAL_USERNAME;

    final transient SSLContext sslContext;
    final OracleWallet wallet;
    private final Map<String, DataSourceCredentials> credentials;
    private final String serviceAlias;

    private Wallet(
            final OracleWallet wallet,
            final Map<String, DataSourceCredentials> credentials,
            final SSLContext sslContext,
            final String serviceAlias) {
        this.wallet = wallet;
        this.credentials = credentials;
        this.sslContext = sslContext;
        this.serviceAlias = serviceAlias;
    }

    static Wallet of(final OracleWallet wallet) throws WalletException {
        try {
            final OracleSecretStore store = wallet.getSecretStore();
            final Map<String, DataSourceCredentials> credentials =
                    DataSourceCredentials.credentials(store);
            final SSLContext sslContext = sslContext(wallet);
            return new Wallet(wallet, credentials, sslContext, null);
        } catch (OracleSecretStoreException | IOException e) {
            throw WalletException.of(e);
        }
    }

    private static SSLContext sslContext(OracleWallet wallet) throws WalletException {
        try {
            /*
             * Register OraclePKIProvider as the final JCE provider, as otherwise {@link OracleWallet}
             * registers OraclePKIProvider as the first provider, which leads to issues, such as an NPE when
             * trying to retrieve the default {@link javax.net.ssl.SSLContext}.
             */
            if (Security.getProvider("OraclePKI") == null) {
                Security.insertProviderAt(new OraclePKIProvider(), Integer.MAX_VALUE);
            }
            final KeyStore walletKeyStore = wallet.getKeyStore();
            /* Don't create a context if there are no certs */
            if (walletKeyStore == null || 0 == walletKeyStore.size()) {
                return null;
            } else {
                TrustManagerFactory tmf =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(walletKeyStore);
                KeyManagerFactory kmf =
                        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(walletKeyStore, null);
                SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                return sslContext;
            }
        } catch (NoSuchAlgorithmException
                | KeyStoreException
                | UnrecoverableKeyException
                | KeyManagementException
                | IOException e) {
            throw WalletException.of(e);
        }
    }

    /**
     * Indicates the database service alias (AKA TNS Alias) associated with this wallet. Since a
     * wallet may hold multiple sets of database credentials, this value identifies which service
     * will be used to configure a data source via {@link #configure(OracleDataSourceAttributes)}
     *
     * @return The service alias associated with this wallet, or null if this wallet is not
     *     associated with a particular service
     * @see #with(String)
     */
    public String serviceAlias() {
        return serviceAlias;
    }

    /**
     * Specialize this wallet to configure the specified service alias (AKA TNS Alias)
     *
     * @param serviceAlias The alias of the database service to configure
     * @return Wallet instance that will additionally configure the user credentials and URL for the
     *     database
     * @see #serviceAlias()
     */
    public Wallet with(final String serviceAlias) {
        return new Wallet(wallet, credentials, sslContext, serviceAlias);
    }

    /**
     * Configure a data source with the state held in this wallet. If a {@link #serviceAlias()} has
     * been identified then any database credentials in the wallet will be used to configure the
     * data source.
     *
     * <p>If the wallet contains a certificate key store and/or trust store these will be used to
     * configure a TLS connection for the data source.
     *
     * <p>
     *
     * @param dataSource The data source to be configured by this wallet
     * @return The supplied data source
     * @throws WalletException if an error occurs accessing the wallet
     */
    public <T extends OracleDataSourceAttributes> T configure(T dataSource) throws WalletException {
        if (sslContext != null) {
            dataSource.sslContext(sslContext);
        }
        if (serviceAlias != null) {
            final DataSourceCredentials credentials = this.credentials.get(serviceAlias);
            if (credentials != null) {
                credentials.configure(dataSource);
            }
        }
        return dataSource;
    }

    public InputStream asInputStream() throws IOException {
        return wallet.getWalletArray(true);
    }

    public static class Builder {

        private final transient KeyStore keyStore;

        /* holds credentials */
        private final transient OracleSecretStore store;

        /* The overall wallet */
        private final transient OracleWallet wallet;

        private Builder(final OracleWallet wallet) throws IOException {
            try {
                this.wallet = wallet;
                this.store = wallet.getSecretStore();
                this.keyStore = wallet.getKeyStore();
            } catch (IOException | OracleSecretStoreException e) {
                throw WalletException.of(e);
            }
        }

        static Builder of(OracleWallet wallet) throws IOException {
            return new Builder(wallet);
        }

        /**
         * Build {@link Wallet} instance
         *
         * @return {@link Wallet} instance
         * @throws WalletException if an error occurs creating the wallet
         */
        public Wallet build() throws WalletException {
            try {
                wallet.setSecretStore(store);
                return Wallet.of(wallet);
            } catch (IOException | OracleSecretStoreException e) {
                throw WalletException.of(e);
            }
        }

        /**
         * Search for a secret of the form {@code oracle.security.client.connect_stringN} whose
         * value matches the specified service alias. The value of N is returned
         *
         * @param serviceAlias The database service alias to search for
         * @return The existing index of the specified service alias, or a new index will be
         *     assigned if no match is found
         * @throws WalletException if an error occurs accessing the wallet
         */
        public int findIndex(final String serviceAlias) throws WalletException {
            return findIndex(store, serviceAlias);
        }

        private int findIndex(final OracleSecretStore store, final String serviceAlias)
                throws WalletException {
            try {
                final String aliasPrefix = CONNECT_STRING;
                final String expected = serviceAlias;
                @SuppressWarnings("unchecked")
                final Enumeration<String> e = store.internalAliases();
                int index = 1;
                while (e.hasMoreElements()) {
                    final String alias = e.nextElement();
                    if (alias.startsWith(aliasPrefix)) {
                        final char[] secret = store.getSecret(alias);
                        if (expected.equals(new String(secret))) {
                            /* found an existing alias */
                            return index;
                        }
                        index++;
                    }
                }
                /* didn't find existing assign new index */
                return index;
            } catch (final OracleSecretStoreException e) {
                throw WalletException.of(e);
            }
        }

        /**
         * Add a {@link Certificate} to the wallet
         *
         * @param alias The alias of the certificate
         * @param cert The certificate
         * @return self
         * @throws WalletException if an error occurs adding the {@link Certificate}
         */
        public Builder set(final String alias, final Certificate cert) throws WalletException {
            try {
                if (keyStore.containsAlias(alias)) {
                    keyStore.deleteEntry(alias);
                }
                if (cert != null) {
                    keyStore.setCertificateEntry(alias, cert);
                }
                return this;
            } catch (final KeyStoreException e) {
                throw WalletException.of(e);
            }
        }

        /**
         * Add a secret to the wallet
         *
         * @param alias The alias of the secret
         * @param secret The secret value
         * @return self
         * @throws WalletException if an error occurs adding the secret
         */
        public Builder set(final String alias, final char[] secret) throws WalletException {
            try {
                if (secret == null) {
                    if (store.containsAlias(alias)) {
                        store.deleteSecret(alias);
                    }
                } else {
                    store.setSecret(alias, secret);
                }
                return this;
            } catch (final OracleSecretStoreException e) {
                throw WalletException.of(e);
            }
        }

        private Builder set(final String prefix, final int index, final char[] value)
                throws WalletException {
            if (index > 0) {
                final String alias = prefix + index;
                set(alias, value);
            }
            return this;
        }

        /**
         * Add the credentials for a database service alias
         *
         * @param serviceAlias The alias of the database service ( as it appears in {@code
         *     TNSNAMES.ORA} )
         * @param user The username of the database user
         * @param password The password of the database user
         * @return self
         * @throws WalletException if an error occurs adding the credentials
         */
        public Builder set(final String serviceAlias, final String user, final char[] password)
                throws WalletException {
            final int index = findIndex(store, serviceAlias);
            set(CONNECT_STRING, index, serviceAlias.toCharArray());
            set(USER, index, user.toCharArray());
            set(PASSWORD, index, password);
            return this;
        }
    }
}
