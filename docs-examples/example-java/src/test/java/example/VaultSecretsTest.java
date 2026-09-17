package example;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The secrets read from Oracle Cloud Vault are supplied as test properties
@MicronautTest
@Requires(missingProperty = "micronaut.test.server.executable")
@Property(name = "SECRET_ONE", value = "Value One")
@Property(name = "SECRET_TWO", value = "value two")
@Property(name = "my-secrets.one", value = "Value One")
@Property(name = "my-secrets.two", value = "value two")
@Property(name = "my-secrets.three", value = "true")
@Property(name = "my-secrets.four", value = "42")
@Property(name = "my-secrets.five", value = "3.16")
public class VaultSecretsTest {

    @Inject
    VaultSecrets vaultSecrets;

    @Inject
    MySecretsConfig mySecretsConfig;

    @Test
    void testSecrets() {
        assertEquals("Value One", vaultSecrets.getSecretOne());
        assertEquals("value two", vaultSecrets.getSecretTwo());
    }

    @Test
    void testSecretsConfiguration() {
        assertEquals("Value One", mySecretsConfig.one());
        assertEquals("value two", mySecretsConfig.two());
        assertTrue(mySecretsConfig.three());
        assertEquals(42, mySecretsConfig.four());
        assertEquals(3.16, mySecretsConfig.five());
    }
}
