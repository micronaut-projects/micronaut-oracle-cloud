package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoBootstrapBeanTest {

    @Test
    void defaultHttpProviderDelegatesToManagedNetty() {
        try (ApplicationContext ctx = ApplicationContext.run()) {
            DemoBootstrapBean demo = ctx.getBean(DemoBootstrapBean.class);
            assertTrue(demo.usesManaged(), "Expected HttpProvider.getDefault() to delegate to ManagedNettyHttpProvider (non-default serializer)");
        }
    }
}
