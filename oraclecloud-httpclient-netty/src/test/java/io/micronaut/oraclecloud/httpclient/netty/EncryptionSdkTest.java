package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.encryption.KmsMasterKey;
import com.oracle.bmc.encryption.KmsMasterKeyProvider;
import com.oracle.bmc.encryption.OciCrypto;
import com.oracle.bmc.encryption.OciCryptoInputStream;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.keymanagement.KmsManagementClient;
import com.oracle.bmc.keymanagement.KmsVaultClient;
import com.oracle.bmc.keymanagement.model.CreateKeyDetails;
import com.oracle.bmc.keymanagement.model.CreateVaultDetails;
import com.oracle.bmc.keymanagement.model.Key;
import com.oracle.bmc.keymanagement.model.KeyShape;
import com.oracle.bmc.keymanagement.model.ScheduleVaultDeletionDetails;
import com.oracle.bmc.keymanagement.model.Vault;
import com.oracle.bmc.keymanagement.requests.CreateKeyRequest;
import com.oracle.bmc.keymanagement.requests.CreateVaultRequest;
import com.oracle.bmc.keymanagement.requests.GetVaultRequest;
import com.oracle.bmc.keymanagement.requests.ScheduleVaultDeletionRequest;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled // takes too long
@Requires(property = "vault.secrets.compartment.ocid")
@Requires(bean = AuthenticationDetailsProvider.class)
@MicronautTest
@Property(name = "use.real.auth", value = "true")
class EncryptionSdkTest {
    @Inject
    KmsVaultClient kmsVaultClient;

    @Inject
    HttpProvider httpProvider;

    @Inject
    BasicAuthenticationDetailsProvider auth;

    @Value("${vault.secrets.compartment.ocid}")
    String compartmentId;

    private Vault vault;
    private KmsManagementClient kmsManagementClient;

    @BeforeEach
    void createVault() throws InterruptedException {
        vault = kmsVaultClient.createVault(CreateVaultRequest.builder()
            .createVaultDetails(CreateVaultDetails.builder()
                .compartmentId(compartmentId)
                .vaultType(CreateVaultDetails.VaultType.Default)
                .displayName(EncryptionSdkTest.class.getName().replace('.', '_'))
                .build())
            .build()).getVault();

        while (vault.getLifecycleState() == Vault.LifecycleState.Creating) {
            System.out.println("Waiting for vault to be created");
            TimeUnit.SECONDS.sleep(10);
            vault = kmsVaultClient.getVault(GetVaultRequest.builder()
                .vaultId(vault.getId())
                .build()).getVault();
        }

        kmsManagementClient = KmsManagementClient.builder()
            .vault(vault)
            .httpProvider(httpProvider)
            .build(auth);
    }

    @AfterEach
    void destroyVault() {
        if (vault != null) {
            kmsVaultClient.scheduleVaultDeletion(ScheduleVaultDeletionRequest.builder()
                .vaultId(vault.getId())
                .scheduleVaultDeletionDetails(ScheduleVaultDeletionDetails.builder()
                    .timeOfDeletion(Date.from(Instant.now().plus(8, ChronoUnit.DAYS)))
                    .build())
                .build());
        }
    }

    @Test
    public void test() throws IOException {
        Key key = kmsManagementClient.createKey(CreateKeyRequest.builder()
            .createKeyDetails(CreateKeyDetails.builder()
                .displayName("test-key")
                .compartmentId(compartmentId)
                .protectionMode(CreateKeyDetails.ProtectionMode.Software)
                .keyShape(KeyShape.builder()
                    .algorithm(KeyShape.Algorithm.Aes)
                    .length(32)
                    .build())
                .build())
            .build()).getKey();

        KmsMasterKey masterKey = new KmsMasterKey(
            auth,
            ((RegionProvider) auth).getRegion().getRegionId(),
            vault.getId(),
            key.getId()
        );
        KmsMasterKeyProvider keyProvider = new KmsMasterKeyProvider(masterKey);

        OciCrypto ociCrypto = new OciCrypto();
        OciCryptoInputStream stream = ociCrypto.createEncryptingStream(keyProvider, new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)));
        byte[] encrypted = stream.readAllBytes();

        OciCryptoInputStream decryptingStream = ociCrypto.createDecryptingStream(keyProvider, new ByteArrayInputStream(encrypted));
        byte[] decrypted = decryptingStream.readAllBytes();

        assertEquals("foo", new String(decrypted, StandardCharsets.UTF_8));
    }
}
