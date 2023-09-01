package io.micronaut.oraclecloud.oke.workload.identity;//package io.micronaut.oraclecloud.core;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import io.micronaut.aop.InterceptedProxy;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@MicronautTest
@Property(name = OracleCloudCoreFactory.OKE_WORKLOAD_IDENTITY_PREFIX + ".enabled", value = StringUtils.TRUE)
public class OkeWorkloadIdentityConfigurationTest {

    @Inject
    OkeWorkloadIdentityConfiguration configuration;

    @Inject
    Provider<BasicAuthenticationDetailsProvider> detailsProvider;

    @Test
    void testConfig() {
        assertTrue(configuration.isEnabled());
        BasicAuthenticationDetailsProvider basicAuthenticationDetailsProvider = detailsProvider.get();
        BasicAuthenticationDetailsProvider target = ((InterceptedProxy<BasicAuthenticationDetailsProvider>) basicAuthenticationDetailsProvider).interceptedTarget();
        assertTrue(target instanceof OkeWorkloadIdentityAuthenticationDetailsProvider);
        ((OkeWorkloadIdentityAuthenticationDetailsProvider) target).refresh();
    }

    @MockBean(BasicAuthenticationDetailsProvider.class)
    BasicAuthenticationDetailsProvider mockBean() {
        return mock(OkeWorkloadIdentityAuthenticationDetailsProvider.class);
    }
}
