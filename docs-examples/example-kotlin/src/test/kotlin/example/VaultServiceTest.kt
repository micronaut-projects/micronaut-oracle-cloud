package example

import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

// The Vault client is created with the mock authentication details provider of example.mock
@MicronautTest
@Requires(missingProperty = "micronaut.test.server.executable")
class VaultServiceTest {

    @Inject
    lateinit var vaultService: VaultService

    @Test
    fun testVaultClientIsInjected() {
        assertNotNull(vaultService.vault)
    }
}
