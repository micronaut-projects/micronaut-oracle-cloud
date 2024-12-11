package io.micronaut.oraclecloud.monitoring.sdk

import com.oracle.bmc.Region
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.monitoring.Monitoring
import com.oracle.bmc.monitoring.MonitoringClient
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import spock.lang.Specification

@MicronautTest(startApplication = false)
@Property(name = "spec.name", value = "MonitorRegionProviderSpec")
@Property(name = "micronaut.metrics.export.oraclecloud.enabled", value = "false")
@Property(name = "oci.vault.config.enabled", value = "false")
@Property(name = "micronaut.config-client.enabled", value = "false")
@Property(name = "datasources.enabled", value = "false")
@Requires(bean = AbstractAuthenticationDetailsProvider.class)
class MonitorRegionProviderSpec extends Specification {

    @Inject
    Monitoring monitoring

    void 'test the' () {
        when:
        String endpoint = monitoring.getEndpoint()

        then:
        Region.EU_JOVANOVAC_1.getEndpoint(MonitoringClient.SERVICE).get() == endpoint
    }

}
