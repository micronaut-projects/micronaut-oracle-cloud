package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.core.util.ConnectionString
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(contextBuilder = OracleCloudVaultPropertySourceImporterLifecycleSpec.MyContextBuilder)
class OracleCloudVaultPropertySourceImporterLifecycleSpec extends Specification {

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

    @Inject Secrets secrets
    @Inject Vaults vaults

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

    void 'importer close releases owned context and can recreate it for subsequent load cycle'() {
        given:
        def raw = ConnectionString.parse('oraclecloud-vault://' + COMPARTMENT_OCID + '/' + VAULT_OCID + '?includes=alpha-.*&retry-attempts=2&retry-delay=10ms')
        def declaration = new OracleCloudVaultImportConfigurationBinder().bind(raw)
        OracleCloudVaultImporterContextFactory importerContextFactory = new OracleCloudVaultImporterContextFactory()

        when:
        def firstContext = importerContextFactory.create(context.environment, declaration, secrets, vaults)
        def first = new OracleCloudVaultPropertySourceLoader(secrets, vaults, firstContext.configuration().discoveryConfiguration).load(firstContext.configuration().vaults)
        firstContext.close()
        def secondContext = importerContextFactory.create(context.environment, declaration, secrets, vaults)
        def second = new OracleCloudVaultPropertySourceLoader(secrets, vaults, secondContext.configuration().discoveryConfiguration).load(secondContext.configuration().vaults)

        then:
        first != null
        second != null
        new String((byte[]) first.get('alpha-v1')) == 'alpha-one'
        new String((byte[]) second.get('alpha-v1')) == 'alpha-one'

        cleanup:
        firstContext?.close()
        secondContext?.close()
    }

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        MyContextBuilder() {
            bootstrapEnvironment(true)
            properties([
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled': true,
                'oci.config.enabled': false,
                'micronaut.metrics.export.oraclecloud.enabled': false,
                'oci.vault.config.retry-attempts': 2,
                'oci.vault.config.retry-delay': '10ms',
                'oci.vault.vaults': [[
                    'ocid': VAULT_OCID,
                    'compartment-ocid': COMPARTMENT_OCID,
                    'includes': ['alpha-.*']
                ]]
            ])
        }
    }
}
