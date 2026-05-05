/*
 * Copyright 2017-2026 original authors
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

import com.oracle.bmc.database.Database;
import com.oracle.bmc.database.requests.GenerateAutonomousDatabaseWalletRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @ParameterizedTest
    @MethodSource("missingWalletPasswordCases")
    void generatesWalletPasswordWhenNotConfigured(String configuredPassword) {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        cfg.setWalletPassword(configuredPassword);

        String walletPassword = cfg.getWalletPassword();

        assertSame(walletPassword, cfg.getWalletPassword());
        assertEquals(32, walletPassword.length());
        assertTrue(walletPassword.matches(".*[A-Za-z].*"));
        assertTrue(walletPassword.matches(".*[0-9].*"));
    }

    @ParameterizedTest
    @MethodSource("configuredWalletPasswordCases")
    void preservesConfiguredWalletPassword(String configuredPassword) {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        cfg.setWalletPassword(configuredPassword);

        assertEquals(configuredPassword, cfg.getWalletPassword());
    }

    @ParameterizedTest
    @MethodSource("missingWalletPasswordCases")
    void sendsGeneratedWalletPasswordToWalletRequest(String configuredPassword) {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        cfg.setOcid("ocid1.autonomousdatabase.oc1..example");
        cfg.setServiceAlias("example_high");
        cfg.setWalletPassword(configuredPassword);
        OracleWalletArchiveProvider provider = new OracleWalletArchiveProvider(databaseCapturingWalletRequest());

        assertThrows(ExpectedRequestCapturedException.class, () -> provider.loadWalletArchive(cfg));
    }

    private static Stream<Arguments> aliasCases() {
        return Stream.of(
            Arguments.of(null, null, "mydb", "mydb_high"),
            Arguments.of(null, "tp", "mydb", "mydb_tp"),
            Arguments.of("custom_alias", "tp", "mydb", "custom_alias")
        );
    }

    private static Stream<Arguments> missingWalletPasswordCases() {
        return Stream.of(
            Arguments.of((String) null),
            Arguments.of(""),
            Arguments.of("   ")
        );
    }

    private static Stream<Arguments> configuredWalletPasswordCases() {
        return Stream.of(
            Arguments.of("micronaut.1"),
            Arguments.of("short1"),
            Arguments.of("abcdefghijklmnopqrstuvwxyz123456")
        );
    }

    private static Database databaseCapturingWalletRequest() {
        return (Database) Proxy.newProxyInstance(
            Database.class.getClassLoader(),
            new Class<?>[] { Database.class },
            (proxy, method, args) -> {
                if ("generateAutonomousDatabaseWallet".equals(method.getName())) {
                    GenerateAutonomousDatabaseWalletRequest request = (GenerateAutonomousDatabaseWalletRequest) args[0];
                    String password = request.getGenerateAutonomousDatabaseWalletDetails().getPassword();
                    assertNotNull(password);
                    assertEquals(32, password.length());
                    assertTrue(password.matches(".*[A-Za-z].*"));
                    assertTrue(password.matches(".*[0-9].*"));
                    throw new ExpectedRequestCapturedException();
                }
                if ("toString".equals(method.getName())) {
                    return "Database proxy";
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private static final class ExpectedRequestCapturedException extends RuntimeException {
    }
}
