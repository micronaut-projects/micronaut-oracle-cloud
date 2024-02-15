package io.micronaut.oraclecloud.core;

import com.oracle.bmc.ClientConfiguration;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest(environments = Environment.ORACLE_CLOUD)
class OracleCloudCoreDefaultConfigTest {

    /*
     * This tests that the default configurations can be setup.
     */
    @Test
    void testDefaultConfig(ClientConfiguration clientConfiguration) {
        assertNotNull(clientConfiguration);
        assertEquals(60000, clientConfiguration.getReadTimeoutMillis());
    }
}
