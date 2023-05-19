package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
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
        expect:
        context.isRunning()
    }

    @MockBean
    Secrets secrets() {
        Mock(Secrets)
    }

    @MockBean
    Vaults vaults() {
        Mock(Vaults)
    }

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        MyContextBuilder() {
            bootstrapEnvironment(true)
        }
    }
}
