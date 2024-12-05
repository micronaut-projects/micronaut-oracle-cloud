package example;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.vault.Vaults;
import com.oracle.bmc.vault.VaultsClient;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@MicronautTest(startApplication = false)
@Property(name = "spec.name", value = "VaultRegionProviderTest")
@Property(name = "micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Requires(bean = AbstractAuthenticationDetailsProvider.class)
class VaultRegionProviderTest {

    @Inject
    Vaults vaults;

    @Test
    void testVaultLoadSecrets() {
        Assertions.assertEquals(Region.EU_JOVANOVAC_1.getEndpoint(VaultsClient.SERVICE).get(), vaults.getEndpoint());
    }

}
