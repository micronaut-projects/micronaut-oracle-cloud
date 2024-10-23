package io.micronaut.oraclecloud.httpclient.apache.core;

import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

public class ManagedJacksonNettyTest extends ApacheNettyTest {
    ApplicationContext ctx;

    @BeforeEach
    public void setUp() {
        ctx = ApplicationContext.run(Map.of("spec.name", "ManagedJacksonNettyTest"));
        Assertions.assertInstanceOf(JacksonSerializer.class, ctx.getBean(ApacheCoreSerializer.class));
    }

    @AfterEach
    public void tearDown() {
        ctx.close();
    }

    @Override
    HttpProvider provider() {
        return ctx.getBean(ApacheCoreHttpProvider.class);
    }
}
