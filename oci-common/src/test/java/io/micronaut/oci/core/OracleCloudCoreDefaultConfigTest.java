package io.micronaut.oci.core;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

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
        assertNotNull(clientConfiguration);
        assertNotNull(provider);
    }
}
