package example;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class VaultImportTest {

    @Test
    void testOptionalImportWithoutVaultConfigurationIsNonFatal() {
        assumeOciConfigAvailable();
        try (ApplicationContext context = ApplicationContext.run(
            Map.of(
                "micronaut.config.import[0]", "optional:env://VAULT_IMPORT_UNUSED",
                "micronaut.metrics.export.oraclecloud.enabled", false
            )
        )) {
            assertNotNull(context);
            assertFalse(context.containsProperty("alpha-v1"));
        }
    }

    private static void assumeOciConfigAvailable() {
        String configPath = System.getProperty("oci.config.path");
        if (configPath == null || configPath.isBlank()) {
            configPath = System.getenv("OCI_CONFIG_FILE");
        }
        assumeTrue(configPath != null && !configPath.isBlank(), "OCI config path required for vault import integration test");
    }
}
