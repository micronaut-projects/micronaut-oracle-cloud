package io.micronaut.oraclecloud.core;

import com.oracle.bmc.ClientConfiguration;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "oci.client.read-timeout-millis", value = "25000")
public class OracleCloudCustomConfigTest {

    @Test
    void testCustomConfig(
            ClientConfiguration clientConfiguration,
            OracleCloudCoreFactory factory) {
        assertNotNull(clientConfiguration);
        assertEquals(25000, clientConfiguration.getReadTimeoutMillis());
    }
}
