package example;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VaultImportTest {

    @Test
    void testOptionalImportWithoutVaultConfigurationIsNonFatal() {
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
}
