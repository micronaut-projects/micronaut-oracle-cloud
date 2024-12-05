package example;

import com.oracle.bmc.Region;
import com.oracle.bmc.vault.Vaults;
import com.oracle.bmc.vault.VaultsClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class VaultRegionProviderTest {

    @Test
    void testVaultLoadSecrets() {

        ApplicationContext context = ApplicationContext.run(
                Map.of(
                    "spec.name", "VaultRegionProviderTest",
                    "micronaut.config-client.enabled", false,
                    "oci.vault.config.enabled", false,
                    "micronaut.metrics.export.oraclecloud.enabled",  false
                ), Environment.ORACLE_CLOUD);

        Vaults vaults = context.getBean(Vaults.class);

        Assertions.assertEquals(Region.EU_JOVANOVAC_1.getEndpoint(VaultsClient.SERVICE).get(), vaults.getEndpoint());
    }

}
