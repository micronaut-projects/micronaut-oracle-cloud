package example;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.oraclecloud.discovery.vault.OracleCloudVaultConfigurationClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Map;

@MicronautTest
@Requires(property = "vault.ocid")
@Requires(property = "vault.secrets.compartment.ocid")
@Requires(property = "vault.secret.name")
@Requires(property = "vault.secret.value")
public class VaultTest {

    @Property(name = "vault.ocid")
    String vaultOcid;

    @Property(name = "vault.secrets.compartment.ocid")
    String compartmentOcid;

    @Property(name = "vault.secret.name")
    String secretName;

    @Property(name = "vault.secret.value")
    String secretValue;

    @Test
    void testVaultLoadSecrets() {
        ArrayList<Map<?,?>> vaults = new ArrayList<>();

        vaults.add(Map.of(
            "ocid", vaultOcid,
            "compartment-ocid", compartmentOcid
        ));

        ApplicationContext context = ApplicationContext.run(
                Map.of(
                    "micronaut.config-client.enabled", true,
                    "oci.vault.config.enabled", true,
                    "oci.vault.vaults", vaults
            ), Environment.ORACLE_CLOUD);

        OracleCloudVaultConfigurationClient client = context.getBean(OracleCloudVaultConfigurationClient.class);
        PropertySource propertySource = Flux.from(client.getPropertySources(null)).blockFirst();
        Assertions.assertNotNull(propertySource);
        Assertions.assertEquals(secretValue, propertySource.get(secretName));
        context.close();
    }

}
