package io.micronaut.oraclecloud.client

import com.oracle.bmc.Region
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.RegionProvider
import com.oracle.bmc.monitoring.MonitoringClient
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@Requires(bean = AuthenticationDetailsProvider)
@Requires(bean = RegionProvider)
@MicronautTest
class OciCustomEndpointSpec extends Specification {

    private static final CUSTOM_ENDPOINT = "https://this-is-my-custom-endpoint.com/test"

    @Inject
    @NonNull
    AuthenticationDetailsProvider authenticationDetailsProvider

    @Inject
    MonitoringClient.Builder client

    void "test get compartment"() {
        when:
        var client = buildClient()

        then:
        client.getEndpoint() == CUSTOM_ENDPOINT
    }

    MonitoringClient buildClient() {
        return client.endpoint(CUSTOM_ENDPOINT).build(authenticationDetailsProvider)
    }

    @Singleton
    @Primary
    @Replaces(RegionProvider.class)
    @Bean(typed = RegionProvider.class)
    static class TestRegionProvider implements RegionProvider {

        @Override
        Region getRegion() {
            return Region.EU_JOVANOVAC_1
        }
    }
}
