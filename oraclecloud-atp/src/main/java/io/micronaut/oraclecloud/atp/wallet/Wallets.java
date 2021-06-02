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

import oracle.security.pki.OracleWallet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Services for reading Oracle {@code cwallet.sso} and {@code ewallet.p12} wallets. */
public class Wallets {

    private static final String CWALLET_SSO = "cwallet.sso";
    private static final String EWALLET_P12 = "ewallet.p12";
    private final OracleWallets wallets;
    private final ByteStreams streams;
    private final Archives archives;

    Wallets(final ByteStreams streams, final OracleWallets wallets, final ZipArchives zipArchives) {
        this.streams = streams;
        this.wallets = wallets;
        this.archives = new Archives(zipArchives);
    }

    /**
     * Create an auto login io.micronaut.oraclecloud.adb.wallet {@link Wallet.Builder}.
     *
     * @return {@link Wallet.Builder} instance
     * @throws IOException if an error occurs creating the io.micronaut.oraclecloud.adb.wallet
     */
    public Wallet.Builder builder() throws IOException {
        final OracleWallet wallet = new OracleWallet();
        wallet.createSSO();
        return Wallet.Builder.of(wallet);
    }

    /**
     * Create a builder to make changes to a wallet.
     *
     * @param wallet the wallet
     * @return the builder
     * @throws IOException if there's a problem reading the wallet
     */
    public Wallet.Builder modify(final Wallet wallet) throws IOException {
        return modify(wallet, null);
    }

    /**
     * Create a builder to make changes to a wallet.
     *
     * @param wallet the wallet
     * @param password the password used to encrypt/decrypt the contents of the wallet
     * @return the builder
     * @throws IOException if there's a problem reading the wallet
     */
    public Wallet.Builder modify(final Wallet wallet, final char[] password) throws IOException {
        return Wallet.Builder.of(wallets.copy(wallet.wallet, password));
    }

    /**
     * Read the contents of a {@code cwallet.sso} io.micronaut.oraclecloud.adb.wallet.
     *
     * @param content The bytes of the io.micronaut.oraclecloud.adb.wallet
     * @return Wallet instance
     * @throws IOException if an error occurs reading or decrypting the io.micronaut.oraclecloud.adb.wallet
     */
    public Wallet read(final InputStream content) throws IOException {
        final OracleWallet autoLoginWallet = wallets.read(streams.asByteArray(content), null);
        final Wallet wallet = Wallet.of(autoLoginWallet);
        return wallet;
    }

    /**
     * Read the contents of the specified io.micronaut.oraclecloud.adb.wallet folder or io.micronaut.oraclecloud.adb.wallet file.
     *
     * @param wallet Path to the io.micronaut.oraclecloud.adb.wallet. If the specified location is a folder then the {@code
     *     cwallet.sso} located within that folder is read. If the location is a file then that file
     *     is read as a auto login io.micronaut.oraclecloud.adb.wallet.
     * @return Wallet instance
     * @throws IOException if an error occurs reading the specified location
     */
    public Wallet read(final Path wallet) throws IOException {
        if (Files.isDirectory(wallet)) {
            Path autoLoginWallet = wallet.resolve(CWALLET_SSO);
            return readWallet(autoLoginWallet);
        }
        return readWallet(wallet);
    }

    private Wallet readWallet(Path wallet) throws IOException {
        try (InputStream content = Files.newInputStream(wallet)) {
            return read(content);
        }
    }

    /**
     * Read the contents of the specified io.micronaut.oraclecloud.adb.wallet folder or io.micronaut.oraclecloud.adb.wallet file.
     *
     * @param wallet Path to the io.micronaut.oraclecloud.adb.wallet. If the specified location is a folder then the {@code
     *     ewallet.p12} located within that folder is read. If the location is a file then that file
     *     is read as a password protected io.micronaut.oraclecloud.adb.wallet.
     * @param password the password used to encrypt/decrypt the contents of the wallet
     * @return Wallet instance
     * @throws IOException if an error occurs reading the specified location
     */
    public Wallet read(final Path wallet, final char[] password) throws IOException {
        if (Files.isDirectory(wallet)) {
            Path passwordProtectedWallet = wallet.resolve(EWALLET_P12);
            return readWallet(passwordProtectedWallet);
        }
        return readWallet(wallet, password);
    }

    private Wallet readWallet(Path wallet, char[] password) throws IOException {
        try (InputStream content = Files.newInputStream(wallet)) {
            return read(content, password);
        }
    }

    /**
     * @return the Archives
     */
    public Archives archives() {
        return archives;
    }

    /**
     * Read the contents of an {@code ewallet.p12} io.micronaut.oraclecloud.adb.wallet.
     *
     * @param content The bytes of the io.micronaut.oraclecloud.adb.wallet
     * @param password The password used to encrypt the contents of the io.micronaut.oraclecloud.adb.wallet
     * @return Wallet instance
     * @throws IOException if an error occurs reading or decrypting the io.micronaut.oraclecloud.adb.wallet
     */
    public Wallet read(final InputStream content, char[] password) throws IOException {
        final OracleWallet passwordProtectedWallet =
                wallets.read(streams.asByteArray(content), password);
        final Wallet wallet = Wallet.of(passwordProtectedWallet);
        return wallet;
    }

    /**
     * Helper class to populate a WalletArchive from a compressed wallet.
     */
    public class Archives {

        private final ZipArchives zipArchives;

        Archives(final ZipArchives zipArchives) {
            this.zipArchives = zipArchives;
        }

        /**
         * Read the a Wallet Archive containing at least a {@code cwallet.sso} and optionally a
         * {@code tnsnames.ora}.
         *
         * @param path The path of the zip archive
         * @return WalletArchive instance
         * @throws IOException if an error occurs reading the archive
         */
        public WalletArchive read(final Path path) throws IOException {
            return read(path, null);
        }

        /**
         * Read the a Wallet Archive containing at least a {@code ewallet.p12} and optionally a
         * {@code tnsnames.ora}.
         *
         * @param path The path of the zip archive
         * @param password The password used to protect {@code ewallet.p12}
         * @return WalletArchive instance
         * @throws IOException if an error occurs reading the archive
         */
        public WalletArchive read(final Path path, char[] password) throws IOException {
            final ZipArchive zip = zipArchives.of(path);
            return walletArchive(zip, password);
        }

        /**
         * Read the a Wallet Archive containing at least a {@code cwallet.sso} and optionally a
         * {@code tnsnames.ora}.
         *
         * @param content The contents of the Wallet archive
         * @return WalletArchive instance
         * @throws IOException if an error occurs reading the archive
         */
        public WalletArchive read(final InputStream content) throws IOException {
            return read(content, null);
        }

        /**
         * Read the a Wallet Archive containing at least a {@code ewallet.p12} and optionally a
         * {@code tnsnames.ora}.
         *
         * @param content The content of the io.micronaut.oraclecloud.adb.wallet archive
         * @param password The password used to protect {@code ewallet.p12}
         * @return WalletArchive instance
         * @throws IOException if an error occurs reading the archive
         */
        public WalletArchive read(final InputStream content, char[] password) throws IOException {
            final ZipArchive zip = zipArchives.of(content);
            return walletArchive(zip, password);
        }

        private WalletArchive walletArchive(ZipArchive archive, char[] password)
                throws IOException {
            final String walletName = password == null ? CWALLET_SSO : EWALLET_P12;
            Wallet wallet = null;
            TNSNames tnsNames = null;

            try (ZipInputStream zip = archive.toZipInputStream()) {
                ZipEntry entry = zip.getNextEntry();
                while (entry != null) {
                    final InputStream entryContent = streams.uncloseable(zip);
                    final String entryName = entry.getName();
                    if (walletName.equalsIgnoreCase(entryName)) {
                        wallet = Wallets.this.read(entryContent, password);
                    } else if (TNSNames.NAME.equalsIgnoreCase(entryName)) {
                        tnsNames = TNSNames.read(entryContent);
                    }
                    entry = zip.getNextEntry();
                }
            }
            return new WalletArchive(wallet, tnsNames);
        }
    }
}
