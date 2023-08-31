package io.micronaut.oraclecloud.core;

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
        Assertions.assertThrows(IllegalArgumentException.class, () -> micronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.createFederationClient(mock(SessionKeySupplier.class)));
    }

}
