package io.micronaut.oraclecloud.client

import com.oracle.bmc.Region
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.RegionProvider
import com.oracle.bmc.monitoring.MonitoringClient
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Factory
import io.micronaut.context.env.Environment
import io.micronaut.oraclecloud.httpclient.netty.MockAuthenticationDetailsProvider
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification


@Requires(bean = AuthenticationDetailsProvider)
@Requires(bean = RegionProvider)
@MicronautTest(startApplication = false)
class OciCustomEndpointSpec extends Specification {

    private static final CUSTOM_ENDPOINT = "https://this-is-my-custom-endpoint.com/test"

    @Inject
    MonitoringClient.Builder client

    void "test custom endpoint"() {

        given:
        ApplicationContext context = ApplicationContext.run([
                "spec.name"            : "OciCustomEndpointSpec",
                "spec.name.provider"   : "OciCustomEndpointSpec",
                "oci.config.enabled"   : "false",
        ], Environment.ORACLE_CLOUD)

        when:
        AuthenticationDetailsProvider authenticationDetailsProvider = context.getBean(AuthenticationDetailsProvider)
        MonitoringClient.Builder clientBuilder = context.getBean(MonitoringClient.Builder)
        var client = clientBuilder.endpoint(CUSTOM_ENDPOINT).build(authenticationDetailsProvider)

        then:
        MockAuthenticationDetailsProvider == authenticationDetailsProvider.getClass()
        client.getEndpoint() == CUSTOM_ENDPOINT

        cleanup:
        context.close()
    }

    void "test custom region"() {

        given:
        ApplicationContext context = ApplicationContext.run([
                "spec.name"            : "OciCustomEndpointSpec",
                "spec.name.provider"   : "OciCustomEndpointSpec",
                "oci.config.enabled"   : "false",
        ], Environment.ORACLE_CLOUD)

        when:
        AuthenticationDetailsProvider authenticationDetailsProvider = context.getBean(AuthenticationDetailsProvider)
        MonitoringClient.Builder clientBuilder = context.getBean(MonitoringClient.Builder)
        var client = clientBuilder.region(Region.US_ASHBURN_1).build(authenticationDetailsProvider)

        then:
        MockAuthenticationDetailsProvider == authenticationDetailsProvider.getClass()
        client.getEndpoint() == "https://telemetry.us-ashburn-1.oraclecloud.com"

        cleanup:
        context.close()
    }

    void "test custom region factory"() {

        given:
        ApplicationContext context = ApplicationContext.run([
                "spec.name"            : "OciCustomEndpointSpecFactory",
                "spec.name.provider"   : "OciCustomEndpointSpecFactory",
                "oci.config.enabled"   : "false",
        ], Environment.ORACLE_CLOUD)

        when:
        AuthenticationDetailsProvider authenticationDetailsProvider = context.getBean(AuthenticationDetailsProvider)
        MonitoringClient client = context.getBean(MonitoringClient)

        then:
        MockAuthenticationDetailsProvider == authenticationDetailsProvider.getClass()
        client.getEndpoint() == "https://telemetry.us-ashburn-1.oraclecloud.com"

        cleanup:
        context.close()
    }

    @Factory
    @Requires(property = "spec.name", value = "OciCustomEndpointSpecFactory")
    static class FactoryReplacement {

        @Singleton
        @Requires(
                classes = [MonitoringClient.class],
                beans = [AbstractAuthenticationDetailsProvider.class]
        )
        @Replaces(MonitoringClient.Builder.class)
        protected MonitoringClient.Builder builder() {
            return new MonitoringClient.Builder(MonitoringClient.SERVICE).region(Region.US_ASHBURN_1);
        }
    }

    @Singleton
    @Primary
    @Replaces(RegionProvider.class)
    @Bean(typed = RegionProvider.class)
    @Requires(property = "spec.name.provider", value = "OciCustomEndpointSpec")
    static class TestRegionProvider implements RegionProvider {

        @Override
        Region getRegion() {
            return Region.EU_JOVANOVAC_1
        }
    }
}
