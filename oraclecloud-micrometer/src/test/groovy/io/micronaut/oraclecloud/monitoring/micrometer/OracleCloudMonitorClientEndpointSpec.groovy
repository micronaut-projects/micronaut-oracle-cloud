package io.micronaut.oraclecloud.monitoring.micrometer

import com.oracle.bmc.Region
import com.oracle.bmc.auth.RegionProvider
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.oraclecloud.monitoring.MonitoringIngestionClient
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@MicronautTest
@Property(name="micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Property(name="spec.name", value = "OracleCloudMonitorClientEndpointSpec")
class OracleCloudMonitorClientEndpointSpec extends Specification {

    @Inject
    ApplicationContext context

    def "test oci sdk metrics client filter request returns exception" () {
        when:
        MonitoringIngestionClient monitoringIngestionClient = context.getBean(MonitoringIngestionClient)
        monitoringIngestionClient.getDelegate()

        then:
        final IllegalArgumentException exception = thrown()
        exception.message == "Region is required for the Monitoring Ingestion client"

    }

    @Singleton
    @BootstrapContextCompatible
    @Primary
    @Replaces(RegionProvider.class)
    @Requires(property = "spec.name", value = "OracleCloudMonitorClientEndpointSpec")
    static class RegionProviderReplacement implements RegionProvider {

        @Override
        Region getRegion() {
            return null
        }
    }

}
