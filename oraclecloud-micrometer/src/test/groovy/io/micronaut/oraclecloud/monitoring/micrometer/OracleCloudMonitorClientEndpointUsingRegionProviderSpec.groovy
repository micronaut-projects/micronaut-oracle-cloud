package io.micronaut.oraclecloud.monitoring.micrometer

import com.oracle.bmc.Region
import com.oracle.bmc.auth.RegionProvider
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.*
import io.micronaut.oraclecloud.monitoring.MonitoringIngestionClient
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@MicronautTest(startApplication = false)
@Property(name="micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Property(name="spec.name", value = "OracleCloudMonitorClientEndpointUsingRegionProviderSpec")
class OracleCloudMonitorClientEndpointUsingRegionProviderSpec extends Specification {

    @Inject
    ApplicationContext context

    def "test oci sdk metrics client filter request returns exception" () {
        when:
        MonitoringIngestionClient monitoringIngestionClient = context.getBean(MonitoringIngestionClient)
        def delegate = monitoringIngestionClient.getDelegate()

        then:
        delegate.getEndpoint() == "https://telemetry-ingestion.eu-jovanovac-1.oraclecloud20.com"
    }

    @Singleton
    @BootstrapContextCompatible
    @Primary
    @Replaces(RegionProvider.class)
    @Requires(property = "spec.name", value = "OracleCloudMonitorClientEndpointUsingRegionProviderSpec")
    static class RegionProviderReplacement implements RegionProvider {

        @Override
        Region getRegion() {
            return Region.EU_JOVANOVAC_1
        }
    }

}
