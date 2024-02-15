package io.micronaut.oraclecloud.core;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = Environment.ORACLE_CLOUD)
@Property(name = "oci.tenantId", value = "something")
@Property(name = "oci.region", value = "ap-mumbai-1")
@Property(name = "oci.passphrase", value = "junk")
class OracleCloudCustomAuthConfigTest {

    @Test
    void testCustomAuth(AuthenticationDetailsProvider provider) {
        assertNotNull(provider);
        assertTrue(provider instanceof SimpleAuthenticationDetailsProvider);
        assertEquals(
                "something",
                provider.getTenantId()
        );
        assertArrayEquals(
                "junk".toCharArray(),
                provider.getPassphraseCharacters()
        );
        assertEquals(
                Region.fromRegionCodeOrId("ap-mumbai-1"),
                ((SimpleAuthenticationDetailsProvider) provider).getRegion()
        );
    }
}
