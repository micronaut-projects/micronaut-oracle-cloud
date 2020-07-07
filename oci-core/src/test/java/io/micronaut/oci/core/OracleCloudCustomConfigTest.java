package io.micronaut.oci.core;

import com.oracle.bmc.ClientConfiguration;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "oracle.cloud.client.readTimeoutMillis", value = "25000")
@Property(name = "oracle.cloud.profile", value = "ADMIN_USER")
public class OracleCloudCustomConfigTest {

    @Test
    void testCustomConfig(
            ClientConfiguration clientConfiguration,
            OracleCloudConfigurationProperties configurationProperties) {
        assertNotNull(clientConfiguration);
        assertEquals(clientConfiguration.getReadTimeoutMillis(), 25000);
        assertTrue(configurationProperties.getProfile().isPresent());
        assertEquals(
                "ADMIN_USER",
                configurationProperties.getProfile().get()
        );
    }
}
