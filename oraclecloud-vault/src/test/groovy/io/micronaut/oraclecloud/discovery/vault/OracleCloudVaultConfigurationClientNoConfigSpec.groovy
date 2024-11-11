package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(contextBuilder = MyContextBuilder)
@Property(name = "micronaut.config-client.enabled", value = "true")
@Property(name = "oci.vault.config.enabled", value = "true")
class OracleCloudVaultConfigurationClientNoConfigSpec extends Specification {
    @Inject ApplicationContext context
    void "context still starts"() {
        when:
        vaults().listSecrets() >>> null
        secrets().getSecretBundle() >>> null

        then:
        context.isRunning()
        context.getBeanDefinition(OracleCloudVaultConfigurationClient.class)

        0 * secrets().getSecretBundle(any())
        0 * vaults().listSecrets(any())
    }

    @MockBean
    @BootstrapContextCompatible
    @Primary
    Secrets secrets() {
        Mock(Secrets)
    }

    @MockBean
    @BootstrapContextCompatible
    @Primary
    Vaults vaults() {
        Mock(Vaults)
    }

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        MyContextBuilder() {
            bootstrapEnvironment(true)
        }
    }
}
