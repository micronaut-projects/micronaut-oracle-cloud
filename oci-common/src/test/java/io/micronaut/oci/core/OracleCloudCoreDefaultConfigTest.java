package io.micronaut.oci.core;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import io.micronaut.test.annotation.MicronautTest;
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
            AuthenticationDetailsProvider provider,
            RegionProvider regionProvider) {
        assertEquals(60000, clientConfiguration.getReadTimeoutMillis());
        assertNotNull(clientConfiguration);
        assertNotNull(provider);
        assertNotNull(regionProvider);
    }
}
