package io.micronaut.oraclecloud.monitoring

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudMeterRegistry
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudRawMeterRegistry
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

@MicronautTest(startApplication = false)
@Property(name = "micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Requires(beans = AuthenticationDetailsProvider)
class OracleCloudMeterRegistryFactoryTest extends Specification {

    def "test it not loads when globally disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.enabled": "false",
        ], Environment.ORACLE_CLOUD)

        expect:
        !context.containsBean(OracleCloudMeterRegistry)
    }

    def "test it not loads when disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.export.oraclecloud.enabled": "false",
        ], Environment.ORACLE_CLOUD)

        expect:
        !context.containsBean(OracleCloudMeterRegistry)
    }

    def "test it loads by default"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.export.oraclecloud.namespace"      : "micronaut_test",
                "micronaut.metrics.export.oraclecloud.applicationName": "micronaut_test"
        ], Environment.ORACLE_CLOUD)

        expect:
        context.containsBean(OracleCloudMeterRegistry)
    }

    def "test raw metrics meter registry"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.export.oraclecloud.namespace"      : "micronaut_test",
                "micronaut.metrics.export.oraclecloud.applicationName": "micronaut_test",
                "micronaut.metrics.export.oraclecloud.raw.enabled"    : "true",
        ], Environment.ORACLE_CLOUD)

        expect:
        context.containsBean(OracleCloudRawMeterRegistry)
    }
}
