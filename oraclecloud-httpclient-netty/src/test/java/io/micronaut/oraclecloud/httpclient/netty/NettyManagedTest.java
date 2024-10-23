package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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
}
