package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoBootstrapBeanTest {

    @Test
    void defaultHttpProviderDelegatesToManagedNetty() {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "oci.netty.use-managed-provider-globally", "true"
        ))) {
            DemoBootstrapBean demo = ctx.getBean(DemoBootstrapBean.class);
            assertTrue(demo.usesManaged(), "Expected HttpProvider.getDefault() to delegate to ManagedNettyHttpProvider (non-default serializer)");
        }
    }
}
