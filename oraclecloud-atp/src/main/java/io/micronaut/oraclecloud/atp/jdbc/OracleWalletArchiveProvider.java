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

import com.oracle.bmc.database.DatabaseClient;
import com.oracle.bmc.database.model.GenerateAutonomousDatabaseWalletDetails;
import com.oracle.bmc.database.requests.GenerateAutonomousDatabaseWalletRequest;
import com.oracle.bmc.database.responses.GenerateAutonomousDatabaseWalletResponse;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.oraclecloud.atp.wallet.WalletArchive;
import io.micronaut.oraclecloud.atp.wallet.WalletModule;

import javax.inject.Singleton;
import java.io.IOException;

/**
 * Oracle archive wallet provider.
 *
 * @author Pavol Gressa
 * @since 2.5
 */
@Singleton
public class OracleWalletArchiveProvider {

    private final DatabaseClient databaseClient;

    public OracleWalletArchiveProvider(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    /**
     * Creates wallet archive based on the {@link AutonomousDatabaseConfiguration}.
     *
     * @param autonomousDatabaseConfiguration configuration
     * @return wallet archive
     */
    public WalletArchive loadWalletArchive(AutonomousDatabaseConfiguration autonomousDatabaseConfiguration) {
        GenerateAutonomousDatabaseWalletDetails.Builder builder = GenerateAutonomousDatabaseWalletDetails.builder()
                .password(autonomousDatabaseConfiguration.getWalletPassword());

        if (autonomousDatabaseConfiguration.getWalletType() != null) {
            builder.generateType(autonomousDatabaseConfiguration.getWalletType());
        }

        final GenerateAutonomousDatabaseWalletResponse walletResponse = databaseClient.generateAutonomousDatabaseWallet(
                GenerateAutonomousDatabaseWalletRequest.builder()
                        .autonomousDatabaseId(autonomousDatabaseConfiguration.getOcid())
                        .generateAutonomousDatabaseWalletDetails(builder.build())
                        .build());


        try {
            final WalletArchive walletArchive = WalletModule.instance()
                    .wallets()
                    .archives()
                    .read(walletResponse.getInputStream())
                    .with(autonomousDatabaseConfiguration.getServiceAlias());
            return walletArchive;
        } catch (IOException e) {
            throw new ConfigurationException("Error creating Oracle Wallet from the response: " + e.getMessage(), e);
        }
    }
}
