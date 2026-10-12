package io.micronaut.oraclecloud.oke.workload.identity;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProxySigningTest {
    @Test
    void proxySigningDoesNotLoadOkeCredentials() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.signing.enabled", false,
            "oci.region", "us-phoenix-1",
            "oci.config.oke-workload-identity.enabled", true
        ))) {
            assertFalse(context.containsBean(OkeWorkloadIdentityAuthenticationDetailsProvider.class));
            assertEquals(1, context.getBeansOfType(AbstractAuthenticationDetailsProvider.class).size());
            assertEquals(Region.US_PHOENIX_1, context.getBean(RegionProvider.class).getRegion());
        }
    }
}
