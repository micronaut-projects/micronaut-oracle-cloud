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

class OracleWallets {
    private final ByteStreams streams;

    OracleWallets(final ByteStreams streams) {
        this.streams = streams;
    }

    OracleWallet read(final byte[] payload, char[] password) throws IOException {
        final OracleWallet wallet = new OracleWallet();
        final InputStream bufferedStream = streams.asInputStream(payload);
        wallet.setWalletArray(bufferedStream, password);
        return wallet;
    }

    /**
     * Duplicate an {@link OracleWallet} instance by serializing it to bytes and de-serializing it
     * back again.
     *
     * <p>This provides the most robust means to preserve the state of the io.micronaut.oraclecloud.adb.wallet.
     *
     * @param existing The {@link OracleWallet} instance to duplicate
     * @return new {@link OracleWallet} instance containing contents of the provided {@link
     *     OracleWallet}
     * @throws IOException
     */
    OracleWallet copy(final OracleWallet existing, char[] password) throws IOException {
        try (final InputStream payload = existing.getWalletArray(true)) {
            final byte[] bytes = streams.asByteArray(payload);
            final OracleWallet duplicate = read(bytes, password);
            return duplicate;
        }
    }
}
