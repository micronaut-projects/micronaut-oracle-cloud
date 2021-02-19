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

/**
 * Entrypoint to the Wallet related API
 *
 * @see #instance()
 * @see #wallets()
 */
public class WalletModule {
    private static final WalletModule INSTANCE =
            new WalletModule(new ByteStreams());
    private final ByteStreams streams;
    private final OracleWallets oracleWallets;
    private final ZipArchives zipArchives;
    private final Wallets wallets;

    private WalletModule(final ByteStreams streams) {
        this.streams = streams;
        this.oracleWallets = new OracleWallets(streams());
        this.zipArchives = new ZipArchives(streams);
        this.wallets = new Wallets(streams(), oracleWallets(), zipArchives());
    }

    /**
     * Retrieve the singleton instance of {@link WalletModule}
     *
     * @return WalletModule instance
     */
    public static WalletModule instance() {
        return INSTANCE;
    }

    ByteStreams streams() {
        return streams;
    }

    OracleWallets oracleWallets() {
        return oracleWallets;
    }

    ZipArchives zipArchives() {
        return zipArchives;
    }

    /**
     * Access the {@link Wallets} service
     *
     * @return Wallets instance
     */
    public Wallets wallets() {
        return wallets;
    }
}
