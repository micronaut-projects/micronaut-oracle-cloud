package io.micronaut.oraclecloud.monitoring

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import io.micrometer.core.instrument.Counter
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudMeterRegistry
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Requires(beans = AuthenticationDetailsProvider.class)
@Property(name="micronaut.metrics.enabled", value = "true")
@Property(name="micronaut.metrics.export.oraclecloudmonitoring.enabled", value = "true")
@Property(name="micronaut.metrics.export.oraclecloudmonitoring.namespace", value = "micronaut_test")
@Property(name="micronaut.metrics.export.oraclecloudmonitoring.step", value = "1s")
class OracleCloudMeterRegistryFactoryTest extends Specification {

    @AutoCleanup
    @Inject
    OracleCloudMeterRegistry cloudMeterRegistry

    def "test it publish metrics to ingestion telemetry endpoint"(){
        given:
        Counter counter = Counter.builder("micronaut.test.counter").
                tag("test", "test").
                description("Testing of micronaut-oraclecloud-monitoring module").
                register(cloudMeterRegistry)

        when:
        counter.increment(5.0)
        sleep(2000)

        then:
        noExceptionThrown()
    }

}
