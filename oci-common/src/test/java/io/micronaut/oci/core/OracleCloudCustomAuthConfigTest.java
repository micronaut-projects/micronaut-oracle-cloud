package io.micronaut.oci.core;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "oci.tenantId", value = "something")
@Property(name = "oci.region", value = "ap-mumbai-1")
public class OracleCloudCustomAuthConfigTest {

    @Test
    void testCustomAuth(AuthenticationDetailsProvider provider) {
        assertNotNull(provider);
        assertTrue(provider instanceof SimpleAuthenticationDetailsProvider);
        assertEquals(
                "something",
                provider.getTenantId()
        );
        assertEquals(
                Region.fromRegionCodeOrId("ap-mumbai-1"),
                ((SimpleAuthenticationDetailsProvider) provider).getRegion()
        );
    }
}
