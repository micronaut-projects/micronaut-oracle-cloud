package io.micronaut.oraclecloud.client

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.keymanagement.KmsManagementClient
import com.oracle.bmc.keymanagement.KmsVaultClient
import com.oracle.bmc.keymanagement.model.*
import com.oracle.bmc.keymanagement.requests.*
import com.oracle.bmc.vault.VaultsClient
import com.oracle.bmc.vault.model.*
import com.oracle.bmc.vault.requests.CreateSecretRequest
import com.oracle.bmc.vault.requests.GetSecretRequest
import com.oracle.bmc.vault.requests.ScheduleSecretDeletionRequest
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.Instant
import java.time.temporal.ChronoUnit

@Requires(property = "test.compartment.id")
@Requires(bean = ConfigFileAuthenticationDetailsProvider)
@MicronautTest
@Stepwise
class OciVaultSpec extends Specification {

    @Shared
    @Property(name = "test.compartment.id")
    String compartmentId

    @Shared
    @Inject
    @NonNull
    ConfigFileAuthenticationDetailsProvider authenticationDetailsProvider

    @Shared KmsVaultClient vaultClient
    @Shared KmsManagementClient keyClient
    @Shared VaultsClient secretsClient

    @Shared String vaultName
    @Shared String vaultId
    @Shared Vault vault
    @Shared String keyId
    @Shared String secretId

    final String secretContent = '{"key":"1","value":"12345"}'
    final String secretContentEncoded = Base64.getEncoder().encodeToString(secretContent.getBytes())

    @spock.lang.Requires({ instance.compartmentId && instance.authenticationDetailsProvider })
    void "create vault"() {
        given:
        vaultClient = buildVaultClient()
        vaultName = "micronaut_test_" + new Random().nextInt(0, Integer.MAX_VALUE)

        var body = CreateVaultDetails.builder()
            .compartmentId(compartmentId)
            .displayName(vaultName)
            .vaultType(CreateVaultDetails.VaultType.Default)
            .build()

        when:
        var response = vaultClient.createVault(CreateVaultRequest.builder().createVaultDetails(body).build())
        vaultId = response.vault.id

        then:
        response.vault.compartmentId == compartmentId
        response.vault.displayName == vaultName
        response.vault.id != null
    }

    void "wait for vault creation"() {
        var request = GetVaultRequest.builder().vaultId(vaultId).build()

        when:
        var response = vaultClient.getWaiters()
                .forVault(request, Vault.LifecycleState.Active, Vault.LifecycleState.PendingDeletion).execute()
        vault = response.vault

        then:
        response.__httpStatusCode__ < 300
        response.vault.id == vaultId
        response.vault.lifecycleState == Vault.LifecycleState.Active
    }

    void "create key"() {
        given:
        keyClient = buildKeyClient(vault)

        var body = CreateKeyDetails.builder()
            .displayName("micronaut_test")
            .keyShape(KeyShape.builder().algorithm(KeyShape.Algorithm.Aes).length(16).build())
            .compartmentId(compartmentId)
            .build()

        when:
        var response = keyClient.createKey(CreateKeyRequest.builder().createKeyDetails(body).build())
        keyId = response.key.id

        then:
        response.__httpStatusCode__ < 300
        response.key.compartmentId == compartmentId
        response.key.id != null
    }

    void "wait for key"() {
        when:
        var request = GetKeyRequest.builder().keyId(keyId).build()
        var response = keyClient.waiters
                .forKey(request, Key.LifecycleState.Enabled, Key.LifecycleState.PendingDeletion).execute()

        then:
        response.__httpStatusCode__ < 300
        response.key.displayName == "micronaut_test"
        response.key.keyShape.algorithm == KeyShape.Algorithm.Aes
        response.key.keyShape.length == 16
    }

    void "create secret"() {
        given:
        secretsClient = buildSecretsClient()

        var expiryDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES))
        var body = CreateSecretDetails.builder()
            .vaultId(vaultId).compartmentId(compartmentId)
            .secretName("test_secret")
            .secretRules([
                    SecretExpiryRule.builder().timeOfAbsoluteExpiry(expiryDate).build(),
                    SecretReuseRule.builder().isEnforcedOnDeletedSecretVersions(true).build()
            ])
            .description("Test secret")
            .secretContent(
                    Base64SecretContentDetails.builder().content(secretContentEncoded).build()
            )
            .keyId(keyId)
            .build()

        when:
        var response = secretsClient.createSecret(CreateSecretRequest.builder().createSecretDetails(body).build())
        secretId = response.secret.id

        then:
        response.secret.secretName == "test_secret"
        response.secret.compartmentId == compartmentId
        response.secret.id != null
    }

    void "wait for secret"() {
        when:
        var request = GetSecretRequest.builder().secretId(secretId).build()
        var response = secretsClient.waiters
                .forSecret(request, Secret.LifecycleState.Active, Secret.LifecycleState.PendingDeletion).execute()

        then:
        response.__httpStatusCode__ < 300
        response.secret.vaultId == vaultId
        response.secret.secretName == "test_secret"
        response.secret.id == secretId
        response.secret.secretRules.size() == 2
        response.secret.lifecycleState == Secret.LifecycleState.Active
        var rule1 = response.secret.secretRules.find{ it instanceof SecretExpiryRule}
        ((SecretExpiryRule) rule1).timeOfAbsoluteExpiry != null
        var rule2 = response.secret.secretRules.find{ it instanceof SecretReuseRule}
        ((SecretReuseRule) rule2).isEnforcedOnDeletedSecretVersions == true
    }

    void "schedule secret deletion"() {
        when:
        var deletionDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES))
        var body = ScheduleSecretDeletionDetails.builder().timeOfDeletion(deletionDate).build()
        var request = ScheduleSecretDeletionRequest.builder().secretId(secretId).scheduleSecretDeletionDetails(body).build()
        var response = secretsClient.scheduleSecretDeletion(request)

        then:
        response.__httpStatusCode__ < 300
    }

    void "schedule key deletion"() {
        when:
        var deletionDate = Date.from(Instant.now().plus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES))
        var body = ScheduleKeyDeletionDetails.builder().timeOfDeletion(deletionDate).build()
        var request = ScheduleKeyDeletionRequest.builder().keyId(keyId).scheduleKeyDeletionDetails(body).build()
        var response = keyClient.scheduleKeyDeletion(request)

        then:
        response.__httpStatusCode__ < 300
        response.key.id == keyId
    }

    void "schedule vault deletion"() {
        given:
        var time = Date.from(Instant.now().plus(7, ChronoUnit.DAYS).plus(5, ChronoUnit.MINUTES))
        var body = ScheduleVaultDeletionDetails.builder().timeOfDeletion(time).build()

        when:
        var request = ScheduleVaultDeletionRequest.builder()
                .scheduleVaultDeletionDetails(body)
                .vaultId(vaultId)
                .build()
        var response = vaultClient.scheduleVaultDeletion(request)

        then:
        response.__httpStatusCode__ < 300
        response.vault != null
        response.vault.compartmentId == compartmentId
        response.vault.displayName == vaultName
        response.vault.id == vaultId
    }

    KmsVaultClient buildVaultClient() {
        return KmsVaultClient.builder().build(authenticationDetailsProvider)
    }

    VaultsClient buildSecretsClient() {
        return VaultsClient.builder().build(authenticationDetailsProvider)
    }

    KmsManagementClient buildKeyClient(Vault vault) {
        return KmsManagementClient.builder()
                .vault(vault)
                .build(authenticationDetailsProvider)
    }

}
