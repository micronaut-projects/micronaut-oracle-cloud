/*
 * Copyright 2017-2025 original authors
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleWalletArchiveProviderTest {

    @Test
    void usesDefaultHighWhenSuffixNotConfigured() {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        // No explicit service alias, no suffix configured
        cfg.setServiceAlias(null);
        cfg.setServiceAliasSuffix(null);

        String result = OracleWalletArchiveProvider.resolveServiceAlias(cfg, "mydb");
        assertEquals("mydb_high", result);
    }

    @Test
    void usesConfiguredSuffixWhenProvided() {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        // No explicit service alias, suffix configured
        cfg.setServiceAlias(null);
        cfg.setServiceAliasSuffix("tp");

        String result = OracleWalletArchiveProvider.resolveServiceAlias(cfg, "mydb");
        assertEquals("mydb_tp", result);
    }

    @Test
    void explicitServiceAliasOverridesSuffixComputation() {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        cfg.setServiceAlias("custom_alias");
        cfg.setServiceAliasSuffix("tp"); // Should be ignored when serviceAlias is set

        String result = OracleWalletArchiveProvider.resolveServiceAlias(cfg, "mydb");
        assertEquals("custom_alias", result);
    }
}
