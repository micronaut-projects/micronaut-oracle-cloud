package example

import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

// The Vault client is created with the mock authentication details provider of example.mock
@MicronautTest
@Requires(missingProperty = "micronaut.test.server.executable")
class VaultServiceSpec extends Specification {

    @Inject VaultService vaultService

    void 'test vault client is injected'() {
        expect:
        vaultService.vault != null
    }
}
