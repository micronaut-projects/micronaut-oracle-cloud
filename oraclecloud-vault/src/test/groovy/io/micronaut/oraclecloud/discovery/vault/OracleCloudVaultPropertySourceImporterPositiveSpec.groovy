package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.PropertySource
import io.micronaut.core.util.ConnectionString
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(contextBuilder = MyContextBuilder)
@Property(name = 'micronaut.config-client.enabled', value = 'true')
@Property(name = 'oci.vault.config.enabled', value = 'true')
class OracleCloudVaultPropertySourceImporterPositiveSpec extends Specification {

    static final String VAULT_OCID = 'ocid1.vault.oc1.phx..'
    static final String COMPARTMENT_OCID = 'ocid1.compartment.oc1..'

    private static final MockVaultSecrets VAULT_SECRETS = new MockVaultSecrets([
        new MockVaultSecrets.Secret(name: 'alpha-v1', id: 'a1', value: 'alpha-one'),
        new MockVaultSecrets.Secret(name: 'alpha-v2', id: 'a2', value: 'alpha-two'),
        new MockVaultSecrets.Secret(name: 'alpha-v3', id: 'a3', value: 'alpha-three'),
        new MockVaultSecrets.Secret(name: 'beta-v1', id: 'b1', value: 'beta-one')
    ])

    @Inject
    ApplicationContext context

    @Inject Secrets secrets
    @Inject Vaults vaults

    void 'importer loads vault secrets from reviewed URI declaration'() {
        given:
        def raw = ConnectionString.parse('oraclecloud-vault://' + COMPARTMENT_OCID + '/' + VAULT_OCID + '?includes=alpha-.*&retry-attempts=2&retry-delay=10ms')
        def declaration = new OracleCloudVaultImportConfigurationBinder().bind(raw)
        OracleCloudVaultImporterContextFactory importerContextFactory = new OracleCloudVaultImporterContextFactory()

        when:
        def importerContext =  importerContextFactory.create(context.environment, declaration, secrets, vaults)
        PropertySource propertySource = new OracleCloudVaultPropertySourceLoader(
            secrets,
            vaults,
            importerContext.configuration().discoveryConfiguration
        ).load(importerContext.configuration().vaults)

        then:
        propertySource != null
        new String((byte[]) propertySource.get('alpha-v1')) == 'alpha-one'
        new String((byte[]) propertySource.get('alpha-v2')) == 'alpha-two'
        new String((byte[]) propertySource.get('alpha-v3')) == 'alpha-three'
        !propertySource.contains('beta-v1')

        cleanup:
        importerContext?.close()
    }

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

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        MyContextBuilder() {
            bootstrapEnvironment(true)
            properties([
                'oci.config.enabled': false,
                'vault.test-mocks.enabled': true,
                'micronaut.metrics.export.oraclecloud.enabled': false,
            ])
        }
    }
}
