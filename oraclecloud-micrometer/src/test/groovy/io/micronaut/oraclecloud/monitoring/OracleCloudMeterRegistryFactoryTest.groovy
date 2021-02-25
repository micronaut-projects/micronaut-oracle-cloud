package io.micronaut.oraclecloud.monitoring

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import io.micrometer.core.instrument.Counter
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudMeterRegistry
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification


@MicronautTest(startApplication = false)
@Property(name = "micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Requires(beans = AuthenticationDetailsProvider.class)
class OracleCloudMeterRegistryFactoryTest extends Specification {

    def "test it not loads when disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.export.oraclecloud.enabled": "false",
        ], Environment.ORACLE_CLOUD)

        expect:
        !context.containsBean(OracleCloudMeterRegistry.class)
    }

    def "test it loads by default"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.export.oraclecloud.namespace"      : "micronaut_test",
                "micronaut.metrics.export.oraclecloud.applicationName": "micronaut_test"
        ], Environment.ORACLE_CLOUD)

        expect:
        context.containsBean(OracleCloudMeterRegistry.class)
    }

    def "test it publish metrics to ingestion telemetry endpoint"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.export.oraclecloud.namespace"      : "micronaut_test",
                "micronaut.metrics.export.oraclecloud.applicationName": "micronaut_test"
        ], Environment.ORACLE_CLOUD)
        OracleCloudMeterRegistry cloudMeterRegistry = context.getBean(OracleCloudMeterRegistry.class)

        when:
        Counter counter = Counter.builder("micronaut.test.counter").
                tag("test", "test").
                description("Testing of micronaut-oraclecloud-monitoring module").
                register(cloudMeterRegistry)
        counter.increment(5.0)
        sleep(2000)

        then:
        noExceptionThrown()
    }
}
