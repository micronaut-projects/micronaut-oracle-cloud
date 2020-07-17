package io.micronaut.oci.core;

import com.oracle.bmc.ClientConfiguration;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "oci.client.readTimeoutMillis", value = "25000")
public class OracleCloudCustomConfigTest {

    @Test
    void testCustomConfig(
            ClientConfiguration clientConfiguration,
            OracleCloudCoreFactory factory) {
        assertNotNull(clientConfiguration);
        assertEquals(clientConfiguration.getReadTimeoutMillis(), 25000);
    }
}
