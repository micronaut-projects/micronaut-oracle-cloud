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

import com.oracle.bmc.database.Database;
import com.oracle.bmc.database.model.GenerateAutonomousDatabaseWalletDetails;
import com.oracle.bmc.database.requests.GenerateAutonomousDatabaseWalletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void buildsWalletRequestWhenPasswordIsOmitted() {
        AutonomousDatabaseConfiguration cfg = new AutonomousDatabaseConfiguration();
        cfg.setOcid("ocid1.autonomousdatabase.oc1..example");

        AtomicReference<GenerateAutonomousDatabaseWalletRequest> walletRequest = new AtomicReference<>();
        Database database = (Database) Proxy.newProxyInstance(
            Database.class.getClassLoader(),
            new Class<?>[] { Database.class },
            (proxy, method, args) -> {
                if ("generateAutonomousDatabaseWallet".equals(method.getName())) {
                    walletRequest.set((GenerateAutonomousDatabaseWalletRequest) args[0]);
                    throw new RequestCapturedException();
                }
                return null;
            });
        OracleWalletArchiveProvider provider = new OracleWalletArchiveProvider(database);

        assertThrows(RequestCapturedException.class, () -> provider.loadWalletArchive(cfg));

        GenerateAutonomousDatabaseWalletRequest request = walletRequest.get();
        assertNotNull(request);
        assertEquals("ocid1.autonomousdatabase.oc1..example", request.getAutonomousDatabaseId());
        GenerateAutonomousDatabaseWalletDetails details = request.getGenerateAutonomousDatabaseWalletDetails();
        assertNull(details.getPassword());
        assertFalse(details.wasPropertyExplicitlySet("password"));
    }

    private static Stream<Arguments> aliasCases() {
        return Stream.of(
            Arguments.of(null, null, "mydb", "mydb_high"),
            Arguments.of(null, "tp", "mydb", "mydb_tp"),
            Arguments.of("custom_alias", "tp", "mydb", "custom_alias")
        );
    }

    private static final class RequestCapturedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
