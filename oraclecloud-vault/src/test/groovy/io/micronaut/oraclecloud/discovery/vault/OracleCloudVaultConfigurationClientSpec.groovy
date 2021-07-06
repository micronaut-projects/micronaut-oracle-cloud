package io.micronaut.oraclecloud.discovery.vault

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.PropertySource
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

/**
 * This spec prerequisite is to have existing vault with one secret configured..
 */
@Requires({ System.getenv("VAULT_OCID") && System.getenv("VAULT_SECRETS_COMPARTMENT_OCID") && System.getenv("VAULT_SECRET_NAME") && System.getenv("VAULT_SECRET_VALUE") })
class OracleCloudVaultConfigurationClientSpec extends Specification {

    @Shared
    String vaultOcid = System.getenv("VAULT_OCID")

    @Shared
    String compartmentOcid = System.getenv("VAULT_SECRETS_COMPARTMENT_OCID")

    @Shared
    String secretName = System.getenv("VAULT_SECRET_NAME")

    @Shared
    String secretValue = System.getenv("VAULT_SECRET_VALUE")

    void "it loads secret from vault"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled': true,
                'oci.vault.vaults': [
                        ['ocid': vaultOcid,
                         'compartment-ocid': compartmentOcid]
                ]])
        def client = ctx.getBean(OracleCloudVaultConfigurationClient.class)

        when:
        PropertySource propertySource = Flux.from(client.getPropertySources(null)).blockFirst()

        then:
        !propertySource.isEmpty()
        propertySource.get(secretName) == secretValue

        cleanup:
        ctx.close()
    }
}


