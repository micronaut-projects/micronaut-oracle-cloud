package io.micronaut.oraclecloud.oke.workload.identity;//package io.micronaut.oraclecloud.core;

import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilderTest {
    @Test
    void testThatIfNotInsideKuberentesPodIllegalArgumentExceptionIsThrown() {
        MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder = new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder();
        micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.circuitBreakerConfig(CircuitBreakerConfiguration.builder().build());
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.createFederationClient(mock(SessionKeySupplier.class)));
        Assertions.assertEquals("Kubernetes service account ca cert doesn't exist.", exception.getMessage());
    }

}
