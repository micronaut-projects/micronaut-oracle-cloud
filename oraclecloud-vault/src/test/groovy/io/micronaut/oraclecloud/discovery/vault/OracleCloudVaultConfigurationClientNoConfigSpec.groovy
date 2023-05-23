package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Requires
import spock.lang.Specification

@MicronautTest(contextBuilder = MyContextBuilder)
@Property(name = "micronaut.config-client.enabled", value = "true")
@Property(name = "oci.vault.config.enabled", value = "true")
@Requires({ System.getenv("VAULT_OCID") && System.getenv("VAULT_SECRETS_COMPARTMENT_OCID") && System.getenv("VAULT_SECRET_NAME") && System.getenv("VAULT_SECRET_VALUE") })
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
