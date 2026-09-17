package example;

import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// The Vault client is created with the mock authentication details provider of example.mock
@MicronautTest
@Requires(missingProperty = "micronaut.test.server.executable")
public class VaultServiceTest {

    @Inject
    VaultService vaultService;

    @Test
    void testVaultClientIsInjected() {
        assertNotNull(vaultService.getVault());
    }
}
