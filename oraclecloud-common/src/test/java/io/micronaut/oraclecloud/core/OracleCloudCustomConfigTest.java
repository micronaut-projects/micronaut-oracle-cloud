package io.micronaut.oraclecloud.core;

import com.oracle.bmc.ClientConfiguration;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = Environment.ORACLE_CLOUD)
@Property(name = "oci.client.readTimeoutMillis", value = "25000")
class OracleCloudCustomConfigTest {

    @Test
    void testCustomConfig(
            ClientConfiguration clientConfiguration,
            OracleCloudCoreFactory factory) {
        assertNotNull(clientConfiguration);
        assertEquals(clientConfiguration.getReadTimeoutMillis(), 25000);
    }
}
