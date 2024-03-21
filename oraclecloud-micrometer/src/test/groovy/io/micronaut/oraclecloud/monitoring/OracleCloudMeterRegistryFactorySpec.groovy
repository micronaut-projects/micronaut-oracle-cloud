package io.micronaut.oraclecloud.monitoring

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import io.micrometer.core.instrument.Counter
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudMeterRegistry
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification

@MicronautTest(startApplication = false)
@Property(name = "micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Requires(beans = AuthenticationDetailsProvider)
@spock.lang.Requires({ System.getenv("MONITORING_COMPARTMENT_OCID") })
class OracleCloudMeterRegistryFactorySpec extends Specification {

    @Shared
    String compartmentOcid = System.getenv("MONITORING_COMPARTMENT_OCID")

    @Shared
    String configProfile = System.getenv("MONITORING_CONFIG_PROFILE")

    def "test it publish metrics to ingestion telemetry endpoint"() {
        given:

        def confVariables = [
                "micronaut.metrics.export.oraclecloud.enabled"        : true,
                "micronaut.metrics.export.oraclecloud.step"           : "1s",
                "micronaut.metrics.export.oraclecloud.namespace"      : "micronaut_test",
                "micronaut.metrics.export.oraclecloud.applicationName": "micronaut_test"
        ]

        if (compartmentOcid != null && !compartmentOcid.isEmpty()) {
            confVariables["micronaut.metrics.export.oraclecloud.compartmentId"] = compartmentOcid
        }
        if (configProfile != null && !configProfile.isEmpty()) {
            confVariables["oci.config.profile"] = configProfile
        }

        ApplicationContext context = ApplicationContext.run(confVariables, Environment.ORACLE_CLOUD)
        OracleCloudMeterRegistry cloudMeterRegistry = context.getBean(OracleCloudMeterRegistry)

        when:
        Counter counter = Counter.builder("micronaut.test.counter").
                tag("test", "test").
                description("Testing of micronaut-oraclecloud-monitoring module").
                register(cloudMeterRegistry)
        counter.increment(5.0)
        sleep(6000)

        then:
        noExceptionThrown()
    }
}
