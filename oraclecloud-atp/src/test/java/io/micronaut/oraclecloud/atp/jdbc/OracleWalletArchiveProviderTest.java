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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OracleWalletArchiveProviderTest {

    @ParameterizedTest
    @MethodSource("aliasCases")
    void resolvesServiceAlias(String explicitAlias, String suffix, String baseName, String expected) {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        cfg.setServiceAlias(explicitAlias);
        cfg.setServiceAliasSuffix(suffix);

        String result = OracleWalletArchiveProvider.resolveServiceAlias(cfg, baseName);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> aliasCases() {
        return Stream.of(
            Arguments.of(null, null, "mydb", "mydb_high"),
            Arguments.of(null, "tp", "mydb", "mydb_tp"),
            Arguments.of("custom_alias", "tp", "mydb", "custom_alias")
        );
    }
}
