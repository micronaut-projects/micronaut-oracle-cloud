package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.Region
import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.secrets.SecretsPaginators
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails
import com.oracle.bmc.secrets.model.SecretBundle
import com.oracle.bmc.secrets.requests.GetSecretBundleByNameRequest
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest
import com.oracle.bmc.secrets.requests.ListSecretBundleVersionsRequest
import com.oracle.bmc.secrets.responses.GetSecretBundleByNameResponse
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse
import com.oracle.bmc.secrets.responses.ListSecretBundleVersionsResponse
import com.oracle.bmc.vault.Vaults
import com.oracle.bmc.vault.VaultsPaginators
import com.oracle.bmc.vault.VaultsWaiters
import com.oracle.bmc.vault.model.SecretSummary
import com.oracle.bmc.vault.requests.CancelSecretDeletionRequest
import com.oracle.bmc.vault.requests.CancelSecretRotationRequest
import com.oracle.bmc.vault.requests.CancelSecretVersionDeletionRequest
import com.oracle.bmc.vault.requests.ChangeSecretCompartmentRequest
import com.oracle.bmc.vault.requests.CreateSecretRequest
import com.oracle.bmc.vault.requests.GetSecretRequest
import com.oracle.bmc.vault.requests.GetSecretVersionRequest
import com.oracle.bmc.vault.requests.ListSecretVersionsRequest
import com.oracle.bmc.vault.requests.ListSecretsRequest
import com.oracle.bmc.vault.requests.RotateSecretRequest
import com.oracle.bmc.vault.requests.ScheduleSecretDeletionRequest
import com.oracle.bmc.vault.requests.ScheduleSecretVersionDeletionRequest
import com.oracle.bmc.vault.requests.UpdateSecretRequest
import com.oracle.bmc.vault.responses.CancelSecretDeletionResponse
import com.oracle.bmc.vault.responses.CancelSecretRotationResponse
import com.oracle.bmc.vault.responses.CancelSecretVersionDeletionResponse
import com.oracle.bmc.vault.responses.ChangeSecretCompartmentResponse
import com.oracle.bmc.vault.responses.CreateSecretResponse
import com.oracle.bmc.vault.responses.GetSecretResponse
import com.oracle.bmc.vault.responses.GetSecretVersionResponse
import com.oracle.bmc.vault.responses.ListSecretVersionsResponse
import com.oracle.bmc.vault.responses.ListSecretsResponse
import com.oracle.bmc.vault.responses.RotateSecretResponse
import com.oracle.bmc.vault.responses.ScheduleSecretDeletionResponse
import com.oracle.bmc.vault.responses.ScheduleSecretVersionDeletionResponse
import com.oracle.bmc.vault.responses.UpdateSecretResponse
import com.oracle.bmc.workrequests.WorkRequest
import groovy.transform.Canonical
import io.micronaut.http.client.exceptions.HttpClientException

/**
 * Mock provider for a Vaults and Secrets client. A list of secrets are provided and the
 * client implementations will return those values from the Vaults.listSecrets method and
 * the Secrets.getSecretBundle method.
 *
 * @author schangj09
 */
class MockVaultSecrets {
    public static final String NEXT_PAGE = "next-page-0"
    public static final String VAULT_OCID = "vault1"

    List<Secret> secretsList
    Vaults vaultsClient
    Secrets secretsClient

    MockVaultSecrets(secretsList) {
        this.secretsList = secretsList
        vaultsClient = new MockVaults()
        secretsClient = new MockSecrets()
    }

    /**
     * Provides two pages of secret summaries. The caller should use pageNum 0 for the first page and
     * pageNum 1 for the next.
     *
     * @param pageNum should be either 0 or 1
     * @return the ListSecretsResponse
     */
    ListSecretsResponse getListSecretsResponse(int pageNum) {
        def builder = ListSecretsResponse.builder()
                .__httpStatusCode__(200)
                .opcRequestId("request-id")
        def summaries = secretsList.collect {
            SecretSummary.builder().vaultId(VAULT_OCID).secretName(it.name).id(it.id).build()
        } as ArrayList

        if (pageNum == 0) {
            builder.opcNextPage(NEXT_PAGE)
            builder.items(summaries.subList(0, 4))
        } else {
            builder.items(summaries.subList(4, summaries.size()))
        }
        return builder.build()
    }

    /**
     * Provides a secret bundle response for the requested secret id. If the secret is not
     * found, then a 404 response will be returned.
     *
     * @param getSecretBundleRequest Request object containing the secret ID.
     * @return A GetSecretBundleResponse object containing the retrieved secret bundle.
     */
    def cnt = 0
    GetSecretBundleResponse getSecretsBundle(GetSecretBundleRequest getSecretBundleRequest) {
        def id = getSecretBundleRequest.getSecretId()
        def s = secretsList.find { it.id == id }
        cnt++
        if (cnt%5 == 3 || cnt%5 == 4) {
            throw new HttpClientException("test exception on Secrets.getSecretsBundle")
        }
        if (s == null) {
            return GetSecretBundleResponse.builder().__httpStatusCode__(404).build()
        }
        def encodedValue = new String(Base64.getEncoder().encode(s.value.getBytes("UTF8")))
        return GetSecretBundleResponse.builder()
                .secretBundle(
                        SecretBundle.builder().secretId(id).secretBundleContent(
                                Base64SecretBundleContentDetails.builder().content(encodedValue).build()
                        ).build()
                ).build()
    }

    @Canonical
    static class Secret {
        String name
        String id
        String value
    }

    class MockVaults implements Vaults {
        @Override
        void refreshClient() {

        }

        @Override
        void setEndpoint(String s) {

        }

        @Override
        String getEndpoint() {
            return null
        }

        @Override
        void setRegion(Region region) {

        }

        @Override
        void setRegion(String s) {

        }

        @Override
        void useRealmSpecificEndpointTemplate(boolean b) {

        }

        @Override
        CancelSecretDeletionResponse cancelSecretDeletion(CancelSecretDeletionRequest cancelSecretDeletionRequest) {
            return null
        }

        @Override
        CancelSecretRotationResponse cancelSecretRotation(CancelSecretRotationRequest cancelSecretRotationRequest) {
            return null
        }

        @Override
        CancelSecretVersionDeletionResponse cancelSecretVersionDeletion(CancelSecretVersionDeletionRequest cancelSecretVersionDeletionRequest) {
            return null
        }

        @Override
        ChangeSecretCompartmentResponse changeSecretCompartment(ChangeSecretCompartmentRequest changeSecretCompartmentRequest) {
            return null
        }

        @Override
        CreateSecretResponse createSecret(CreateSecretRequest createSecretRequest) {
            return null
        }

        @Override
        GetSecretResponse getSecret(GetSecretRequest getSecretRequest) {
            return null
        }

        @Override
        GetSecretVersionResponse getSecretVersion(GetSecretVersionRequest getSecretVersionRequest) {
            return null
        }

        @Override
        ListSecretVersionsResponse listSecretVersions(ListSecretVersionsRequest listSecretVersionsRequest) {
            return null
        }

        @Override
        ListSecretsResponse listSecrets(ListSecretsRequest listSecretsRequest) {
            return getListSecretsResponse(listSecretsRequest.getPage() == null ? 0 : 1)
        }

        @Override
        RotateSecretResponse rotateSecret(RotateSecretRequest rotateSecretRequest) {
            return null
        }

        @Override
        ScheduleSecretDeletionResponse scheduleSecretDeletion(ScheduleSecretDeletionRequest scheduleSecretDeletionRequest) {
            return null
        }

        @Override
        ScheduleSecretVersionDeletionResponse scheduleSecretVersionDeletion(ScheduleSecretVersionDeletionRequest scheduleSecretVersionDeletionRequest) {
            return null
        }

        @Override
        UpdateSecretResponse updateSecret(UpdateSecretRequest updateSecretRequest) {
            return null
        }

        @Override
        VaultsWaiters getWaiters() {
            return null
        }

        @Override
        VaultsWaiters newWaiters(WorkRequest workRequest) {
            return null
        }

        @Override
        VaultsPaginators getPaginators() {
            return null
        }

        @Override
        void close() throws Exception {

        }
    }

    class MockSecrets implements Secrets {
        @Override
        String getEndpoint() {
            return null
        }

        @Override
        void refreshClient() {

        }

        @Override
        void setEndpoint(String s) {

        }

        @Override
        void setRegion(Region region) {

        }

        @Override
        void setRegion(String s) {

        }

        @Override
        void useRealmSpecificEndpointTemplate(boolean b) {

        }

        @Override
        GetSecretBundleResponse getSecretBundle(GetSecretBundleRequest getSecretBundleRequest) {
            return getSecretsBundle(getSecretBundleRequest)
        }

        @Override
        GetSecretBundleByNameResponse getSecretBundleByName(GetSecretBundleByNameRequest getSecretBundleByNameRequest) {
            return null
        }

        @Override
        ListSecretBundleVersionsResponse listSecretBundleVersions(ListSecretBundleVersionsRequest listSecretBundleVersionsRequest) {
            return null
        }

        @Override
        SecretsPaginators getPaginators() {
            return null
        }

        @Override
        void close() throws Exception {

        }
    }
}
