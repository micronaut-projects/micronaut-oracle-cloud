package io.micronaut.oraclecloud.httpclient.apache;

import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

public class ManagedSerdeNettyTest extends NettyTest {
    ApplicationContext ctx;

    @BeforeEach
    public void setUp() {
        ctx = ApplicationContext.run(Map.of("spec.name", "ManagedSerdeNettyTest"));
        Assertions.assertInstanceOf(SerdeSerializer.class, ctx.getBean(ApacheSerializer.class));
    }

    @AfterEach
    public void tearDown() {
        ctx.close();
    }

    @Override
    HttpProvider provider() {
        return ctx.getBean(ApacheHttpProvider.class);
    }
}
