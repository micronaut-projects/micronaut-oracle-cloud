package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

@Deprecated
public class LegacyNettyManagedTest extends NettyManagedTest {
    @Override
    @BeforeEach
    public void setUp() {
        ctx = ApplicationContext.run(Map.of("oci.netty.legacy-netty-client", true));
    }

    @Override
    @Test
    @Disabled // response filter order was fixed in the new client impl
    void simpleRequestTestFilters() throws Exception {
        super.simpleRequestTestFilters();
    }
}
