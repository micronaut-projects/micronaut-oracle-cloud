package io.micronaut.oraclecloud.core;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class OracleCloudCoreDefaultConfigTest {

    /*
     * This tests that the default configurations can be setup.
     */
    @Test
    void testDefaultConfig(
            ClientConfiguration clientConfiguration,
            AuthenticationDetailsProvider provider) {
        assertEquals(60000, clientConfiguration.getReadTimeoutMillis());
        assertNotNull(clientConfiguration);
        assertNotNull(provider);
    }
}
