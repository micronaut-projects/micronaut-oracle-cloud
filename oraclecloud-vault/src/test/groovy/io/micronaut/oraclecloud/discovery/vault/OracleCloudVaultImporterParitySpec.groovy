package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.env.PropertySource
import io.micronaut.core.util.ConnectionString
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import spock.lang.Specification

@MicronautTest(contextBuilder = OracleCloudVaultImporterParitySpec.MyContextBuilder)
class OracleCloudVaultImporterParitySpec extends Specification {

    private static final List<MockVaultSecrets.Secret> FIRST_VAULT_SECRETS = [
        new MockVaultSecrets.Secret(name: 'shared-secret', id: 's1', value: 'shared-one'),
        new MockVaultSecrets.Secret(name: 'alpha-v1', id: 'a1', value: 'alpha-one'),
        new MockVaultSecrets.Secret(name: 'alpha-v2', id: 'a2', value: 'alpha-two'),
        new MockVaultSecrets.Secret(name: 'beta-v1', id: 'b1', value: 'beta-one'),
        new MockVaultSecrets.Secret(name: 'beta-v2', id: 'b2', value: 'beta-two'),
        new MockVaultSecrets.Secret(name: 'gamma-v1', id: 'g1', value: 'gamma-one'),
    ]

    private static final List<MockVaultSecrets.Secret> SECOND_VAULT_SECRETS = [
        new MockVaultSecrets.Secret(name: 'shared-secret', id: 's2', value: 'shared-two'),
        new MockVaultSecrets.Secret(name: 'delta-v1', id: 'd1', value: 'delta-one'),
        new MockVaultSecrets.Secret(name: 'delta-v2', id: 'd2', value: 'delta-two'),
        new MockVaultSecrets.Secret(name: 'omega-v1', id: 'o1', value: 'omega-one'),
        new MockVaultSecrets.Secret(name: 'omega-v2', id: 'o2', value: 'omega-two'),
    ]

    private static final OracleCloudVaultConfigurationClientSpecCharacterization.TrackingMockVaultSecrets FIRST_VAULT =
        new OracleCloudVaultConfigurationClientSpecCharacterization.TrackingMockVaultSecrets(FIRST_VAULT_SECRETS, 'vault-one')
    private static final OracleCloudVaultConfigurationClientSpecCharacterization.TrackingMockVaultSecrets SECOND_VAULT =
        new OracleCloudVaultConfigurationClientSpecCharacterization.TrackingMockVaultSecrets(SECOND_VAULT_SECRETS, 'vault-two')

    @Inject
    ApplicationContext context

    @Inject Secrets secrets
    @Inject Vaults vaults

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Secrets secrets() {
        new OracleCloudVaultConfigurationClientSpecCharacterization.DelegatingSecretsClient([FIRST_VAULT, SECOND_VAULT])
    }

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Vaults vaults() {
        new OracleCloudVaultConfigurationClientSpecCharacterization.DelegatingVaultsClient([
            (MyContextBuilder.FIRST_VAULT_OCID): FIRST_VAULT,
            (MyContextBuilder.SECOND_VAULT_OCID): SECOND_VAULT,
        ])
    }

    void setup() {
        FIRST_VAULT.reset()
        SECOND_VAULT.reset()
    }

    void 'importer compatible loading matches legacy filtering ordering and retry behavior'() {
        given:
        def configuration = context.getBean(OracleCloudVaultConfiguration)
        def legacyClient = new OracleCloudVaultConfigurationClient(configuration, null, secrets, vaults)
        def raw = ConnectionString.parse('oraclecloud-vault://' + MyContextBuilder.COMPARTMENT_OCID + '/' + MyContextBuilder.FIRST_VAULT_OCID + '?includes=alpha-.*,beta-v1,shared-secret&retry-attempts=2&retry-delay=10ms')
        def declaration = new OracleCloudVaultImportConfigurationBinder().bind(raw)
        OracleCloudVaultImporterContextFactory importerContextFactory = new OracleCloudVaultImporterContextFactory()

        when:
        PropertySource legacy = Flux.from(legacyClient.getPropertySources(null)).blockFirst()
        FIRST_VAULT.reset()
        SECOND_VAULT.reset()
        def importerContext = importerContextFactory.create(context.environment, declaration, secrets, vaults)
        PropertySource imported = new OracleCloudVaultPropertySourceLoader(
            secrets,
            vaults,
            importerContext.configuration().discoveryConfiguration
        ).load(importerContext.configuration().vaults)

        then:
        legacy != null
        imported != null
        imported.get('alpha-v1') instanceof byte[]
        new String((byte[]) imported.get('alpha-v1')) == new String((byte[]) legacy.get('alpha-v1'))
        imported.contains('alpha-v2') == legacy.contains('alpha-v2')
        FIRST_VAULT.listRequestPages == [null, MockVaultSecrets.NEXT_PAGE]
        FIRST_VAULT.secretBundleAttempts >= FIRST_VAULT.filteredIds.size()
        FIRST_VAULT.filteredIds.containsAll(['s1', 'a1', 'b1'])

        cleanup:
        importerContext?.close()
    }

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        static final String FIRST_VAULT_OCID = 'vault-one'
        static final String SECOND_VAULT_OCID = 'vault-two'
        static final String COMPARTMENT_OCID = 'compartment-one'

        MyContextBuilder() {
            bootstrapEnvironment(true)
            properties([
                'micronaut.config-import[0].provider': 'oraclecloud-vault',
                'oci.vault.config.retry-attempts': 2,
                'oci.vault.config.retry-delay': '10ms',
                'oci.vault.vaults': [
                    [
                        'ocid': FIRST_VAULT_OCID,
                        'compartment-ocid': COMPARTMENT_OCID,
                        'includes': ['alpha-.*', 'beta-v1', 'shared-secret'],
                        'excludes': ['alpha-v2'],
                    ],
                    [
                        'ocid': SECOND_VAULT_OCID,
                        'compartment-ocid': COMPARTMENT_OCID,
                        'includes': ['delta-v1', 'shared-secret'],
                        'excludes': ['omega-.*'],
                    ],
                ],
                'micronaut.metrics.export.oraclecloud.enabled': false,
                'oci.config.enabled': false,
                'vault.test-mocks.enabled': true,
            ])
        }
    }
}
