package example

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

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
class VaultSecretsSpec extends Specification {

    @Inject VaultSecrets vaultSecrets
    @Inject MySecretsConfig mySecretsConfig

    void 'test secrets'() {
        expect:
        vaultSecrets.secretOne == 'Value One'
        vaultSecrets.secretTwo == 'value two'
    }

    void 'test secrets configuration'() {
        expect:
        mySecretsConfig.one == 'Value One'
        mySecretsConfig.two == 'value two'
        mySecretsConfig.three
        mySecretsConfig.four == 42
        mySecretsConfig.five == 3.16d
    }
}
