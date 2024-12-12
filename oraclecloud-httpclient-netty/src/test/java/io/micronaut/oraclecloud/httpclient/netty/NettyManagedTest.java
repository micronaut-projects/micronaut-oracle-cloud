package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.util.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

public class NettyManagedTest extends NettyUnmanagedTest {
    ApplicationContext ctx;

    @BeforeEach
    public void setUp() {
        ctx = ApplicationContext.run();
    }

    @AfterEach
    public void tearDown() {
        ctx.close();
    }

    @Override
    HttpProvider provider() {
        return ctx.getBean(HttpProvider.class);
    }

    @DisabledIfEnvironmentVariable(named = "CI", matches = StringUtils.TRUE,
            disabledReason = "It is flaky https://ge.micronaut.io/scans/tests?tests.container=io.micronaut.oraclecloud.httpclient.netty.NettyManagedTest&tests.test=connectionReuse()")
    @Test
    public void connectionReuse() throws Exception {
        super.connectionReuse();
    }
}
