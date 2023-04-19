package example;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import io.micrometer.core.instrument.Counter;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudMeterRegistry;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;

import java.util.Map;

@MicronautTest
@Requires(beans = AuthenticationDetailsProvider.class)
@Property(name = "micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Requires(property = "monitoring.compartment.ocid")
class MicrometerTest {

    @Property(name = "monitoring.compartment.ocid")
    String compartmentOcid;

    void testMicrometer() {
        Assertions.assertDoesNotThrow(() -> {
            ApplicationContext context = ApplicationContext.run(Map.of(
                "micronaut.metrics.export.oraclecloud.namespace", "micronaut_test",
                "micronaut.metrics.export.oraclecloud.applicationName", "micronaut_test",
                "micronaut.metrics.export.oraclecloud.compartmentId", compartmentOcid,
                "micronaut.metrics.export.oraclecloud.enabled", false
            ), Environment.ORACLE_CLOUD);

            OracleCloudMeterRegistry cloudMeterRegistry = context.getBean(OracleCloudMeterRegistry.class);

            Counter counter = Counter.builder("micronaut.test.counter").
                tag("test", "test").
                description("Testing of micronaut-oraclecloud-monitoring module").
                register(cloudMeterRegistry);
            counter.increment(5.0);
            Thread.sleep(2000);
        });
    }

}
