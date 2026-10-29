package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse
import com.oracle.bmc.vault.Vaults
import com.oracle.bmc.vault.responses.ListSecretsResponse
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.env.PropertySource
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import spock.lang.Specification

@MicronautTest(contextBuilder = OracleCloudVaultConfigurationClientSpecCharacterization.MyContextBuilder)
class OracleCloudVaultConfigurationClientSpecCharacterization extends Specification {

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

    private static final TrackingMockVaultSecrets FIRST_VAULT = new TrackingMockVaultSecrets(FIRST_VAULT_SECRETS, 'vault-one')
    private static final TrackingMockVaultSecrets SECOND_VAULT = new TrackingMockVaultSecrets(SECOND_VAULT_SECRETS, 'vault-two')

    @Inject
    ApplicationContext context

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Secrets secrets() {
        new DelegatingSecretsClient([FIRST_VAULT, SECOND_VAULT])
    }

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Vaults vaults() {
        new DelegatingVaultsClient([
            (MyContextBuilder.FIRST_VAULT_OCID): FIRST_VAULT,
            (MyContextBuilder.SECOND_VAULT_OCID): SECOND_VAULT,
        ])
    }

    void setup() {
        FIRST_VAULT.reset()
        SECOND_VAULT.reset()
    }

    void 'it preserves pagination retry filtering and later-vault-wins semantics'() {
        when:
        def client = context.getBean(OracleCloudVaultConfigurationClient)
        PropertySource propertySource = Flux.from(client.getPropertySources(null)).blockFirst()

        then:
        propertySource != null
        FIRST_VAULT.listRequestPages == [null, MockVaultSecrets.NEXT_PAGE]
        SECOND_VAULT.listRequestPages == [null, MockVaultSecrets.NEXT_PAGE]
        FIRST_VAULT.secretBundleAttempts >= FIRST_VAULT.filteredIds.size()
        FIRST_VAULT.filteredIds.containsAll(['s1', 'a1', 'b1'])
        SECOND_VAULT.filteredIds == ['s2', 'd1']

        propertySource.get('alpha-v1') instanceof byte[]
        new String((byte[]) propertySource.get('alpha-v1')) == 'alpha-one'
        propertySource.get('delta-v1') instanceof byte[]
        new String((byte[]) propertySource.get('delta-v1')) == 'delta-one'
        propertySource.get('shared-secret') instanceof byte[]
        !propertySource.contains('alpha-v2')
        !propertySource.contains('beta-v2')
        !propertySource.contains('delta-v2')
        !propertySource.contains('omega-v2')
        !propertySource.contains('omega-v1')
    }

    void 'it returns no property source when discovery is disabled'() {
        when:
        def disabledConfiguration = new OracleCloudVaultConfiguration()
        disabledConfiguration.discoveryConfiguration.enabled = false
        disabledConfiguration.vaults = [new OracleCloudVaultConfiguration.OracleCloudVault().tap {
            ocid = MyContextBuilder.FIRST_VAULT_OCID
            compartmentOcid = MyContextBuilder.COMPARTMENT_OCID
        }]
        def client = new OracleCloudVaultConfigurationClient(
            disabledConfiguration,
            null,
            FIRST_VAULT.secretsClient,
            FIRST_VAULT.vaultsClient
        )

        then:
        Flux.from(client.getPropertySources(null)).blockFirst() == null
    }

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        static final String FIRST_VAULT_OCID = 'vault-one'
        static final String SECOND_VAULT_OCID = 'vault-two'
        static final String COMPARTMENT_OCID = 'compartment-one'

        MyContextBuilder() {
            bootstrapEnvironment(true)
            properties([
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled': true,
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
            ])
        }
    }

    static final class TrackingMockVaultSecrets extends MockVaultSecrets {
        final String vaultId
        final List<String> listRequestPages = []
        final List<String> filteredIds = []
        int secretBundleAttempts = 0

        TrackingMockVaultSecrets(List<Secret> secretsList, String vaultId) {
            super(secretsList)
            this.vaultId = vaultId
        }

        void reset() {
            listRequestPages.clear()
            filteredIds.clear()
            secretBundleAttempts = 0
            cnt = 0
        }

        @Override
        ListSecretsResponse getListSecretsResponse(int pageNum) {
            listRequestPages << (pageNum == 0 ? null : MockVaultSecrets.NEXT_PAGE)
            return super.getListSecretsResponse(pageNum)
        }

        @Override
        GetSecretBundleResponse getSecretsBundle(GetSecretBundleRequest getSecretBundleRequest) {
            secretBundleAttempts++
            filteredIds << getSecretBundleRequest.secretId
            return super.getSecretsBundle(getSecretBundleRequest)
        }
    }

    static final class DelegatingVaultsClient implements Vaults {
        private final Map<String, TrackingMockVaultSecrets> delegates

        DelegatingVaultsClient(Map<String, TrackingMockVaultSecrets> delegates) {
            this.delegates = delegates
        }

        @Override
        ListSecretsResponse listSecrets(com.oracle.bmc.vault.requests.ListSecretsRequest request) {
            delegates.get(request.vaultId).vaultsClient.listSecrets(request)
        }

        @Override
        void close() throws Exception { }

        @Override
        void refreshClient() { }
        @Override
        void setEndpoint(String s) { }
        @Override
        String getEndpoint() { null }
        @Override
        void setRegion(com.oracle.bmc.Region region) { }
        @Override
        void setRegion(String s) { }
        @Override
        void useRealmSpecificEndpointTemplate(boolean b) { }
        @Override
        com.oracle.bmc.vault.responses.CancelSecretDeletionResponse cancelSecretDeletion(com.oracle.bmc.vault.requests.CancelSecretDeletionRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.CancelSecretRotationResponse cancelSecretRotation(com.oracle.bmc.vault.requests.CancelSecretRotationRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.CancelSecretVersionDeletionResponse cancelSecretVersionDeletion(com.oracle.bmc.vault.requests.CancelSecretVersionDeletionRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.ChangeSecretCompartmentResponse changeSecretCompartment(com.oracle.bmc.vault.requests.ChangeSecretCompartmentRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.CreateSecretResponse createSecret(com.oracle.bmc.vault.requests.CreateSecretRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.GetSecretResponse getSecret(com.oracle.bmc.vault.requests.GetSecretRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.GetSecretVersionResponse getSecretVersion(com.oracle.bmc.vault.requests.GetSecretVersionRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.ListSecretVersionsResponse listSecretVersions(com.oracle.bmc.vault.requests.ListSecretVersionsRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.RotateSecretResponse rotateSecret(com.oracle.bmc.vault.requests.RotateSecretRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.ScheduleSecretDeletionResponse scheduleSecretDeletion(com.oracle.bmc.vault.requests.ScheduleSecretDeletionRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.ScheduleSecretVersionDeletionResponse scheduleSecretVersionDeletion(com.oracle.bmc.vault.requests.ScheduleSecretVersionDeletionRequest request) { null }
        @Override
        com.oracle.bmc.vault.responses.UpdateSecretResponse updateSecret(com.oracle.bmc.vault.requests.UpdateSecretRequest request) { null }
        @Override
        com.oracle.bmc.vault.VaultsWaiters getWaiters() { null }
        @Override
        com.oracle.bmc.vault.VaultsWaiters newWaiters(com.oracle.bmc.workrequests.WorkRequest workRequest) { null }
        @Override
        com.oracle.bmc.vault.VaultsPaginators getPaginators() { null }
    }

    static final class DelegatingSecretsClient implements Secrets {
        private final Map<String, TrackingMockVaultSecrets> bySecretId

        DelegatingSecretsClient(List<TrackingMockVaultSecrets> delegates) {
            this.bySecretId = delegates.collectMany { delegate ->
                delegate.secretsList.collect { [(it.id): delegate] }
            }.collectEntries { it }
        }

        @Override
        GetSecretBundleResponse getSecretBundle(com.oracle.bmc.secrets.requests.GetSecretBundleRequest request) {
            bySecretId.get(request.secretId).secretsClient.getSecretBundle(request)
        }

        @Override
        void close() throws Exception { }

        @Override
        String getEndpoint() { null }
        @Override
        void refreshClient() { }
        @Override
        void setEndpoint(String s) { }
        @Override
        void setRegion(com.oracle.bmc.Region region) { }
        @Override
        void setRegion(String s) { }
        @Override
        void useRealmSpecificEndpointTemplate(boolean b) { }
        @Override
        com.oracle.bmc.secrets.responses.GetSecretBundleByNameResponse getSecretBundleByName(com.oracle.bmc.secrets.requests.GetSecretBundleByNameRequest request) { null }
        @Override
        com.oracle.bmc.secrets.responses.ListSecretBundleVersionsResponse listSecretBundleVersions(com.oracle.bmc.secrets.requests.ListSecretBundleVersionsRequest request) { null }
        @Override
        com.oracle.bmc.secrets.SecretsPaginators getPaginators() { null }
    }
}
