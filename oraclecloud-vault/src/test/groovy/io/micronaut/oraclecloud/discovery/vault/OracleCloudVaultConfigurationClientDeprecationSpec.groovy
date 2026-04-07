package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import spock.lang.Specification

@MicronautTest(contextBuilder = OracleCloudVaultConfigurationClientDeprecationSpec.MyContextBuilder)
class OracleCloudVaultConfigurationClientDeprecationSpec extends Specification {

    static final String VAULT_OCID = 'vault1'
    static final String COMPARTMENT_OCID = 'compartment1'

    private static final MockVaultSecrets VAULT_SECRETS = new MockVaultSecrets([
        new MockVaultSecrets.Secret(name: 'alpha-v1', id: 'a1', value: 'alpha-one'),
        new MockVaultSecrets.Secret(name: 'alpha-v2', id: 'a2', value: 'alpha-two'),
        new MockVaultSecrets.Secret(name: 'alpha-v3', id: 'a3', value: 'alpha-three'),
        new MockVaultSecrets.Secret(name: 'beta-v1', id: 'b1', value: 'beta-one')
    ])

    @Inject
    ApplicationContext context

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Secrets secrets() {
        VAULT_SECRETS.secretsClient
    }

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Vaults vaults() {
        VAULT_SECRETS.vaultsClient
    }

    void 'legacy path remains active and loads property sources'() {
        given:
        def client = context.getBean(OracleCloudVaultConfigurationClient)

        when:
        def propertySource = Flux.from(client.getPropertySources(context.environment)).blockFirst()

        then:
        propertySource != null
        new String((byte[]) propertySource.get('alpha-v1')) == 'alpha-one'
    }

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        MyContextBuilder() {
            bootstrapEnvironment(true)
            properties([
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled': true,
                'oci.vault.config.retry-attempts': 2,
                'oci.vault.config.retry-delay': '10ms',
                'oci.vault.vaults': [[
                    'ocid': VAULT_OCID,
                    'compartment-ocid': COMPARTMENT_OCID,
                    'includes': ['alpha-.*']
                ]],
                'oci.config.enabled': false,
                'micronaut.metrics.export.oraclecloud.enabled': false,
            ])
        }
    }
}
