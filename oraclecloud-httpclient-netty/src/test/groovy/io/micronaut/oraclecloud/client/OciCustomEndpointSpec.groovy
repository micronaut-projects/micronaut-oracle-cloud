package io.micronaut.oraclecloud.client

import com.oracle.bmc.Region
import com.oracle.bmc.auth.AuthCachingPolicy
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.RegionProvider
import com.oracle.bmc.monitoring.MonitoringClient
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
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
@Property(name = "spec.name", value = "OciCustomEndpointSpec")
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
    @Requires(property = "spec.name", notEquals = "OciCustomEndpointSpec")
    static class TestRegionProvider implements RegionProvider {

        @Override
        Region getRegion() {
            return Region.EU_JOVANOVAC_1
        }
    }

    @Singleton
    @Replaces(ConfigFileAuthenticationDetailsProvider.class)
    @Primary
    @Requires(property = "spec.name", notEquals = "OciCustomEndpointSpec")
    static class MockAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {
        private static final String DUMMY_PEM_KEY = """
-----BEGIN RSA PRIVATE KEY-----
MIIBOAIBAAJAUczpZlq0T4QOr4F1RAg/lp0CJLn56ldrmis7bDQ1+XiC3/j7DzhP
oLCd2PWHU/jniJdWAw6wESix/nb0xs/EiQIDAQABAkAqmNqyQmnDPrGnE3NNij4S
4JBNL8vFDOEr13eKUWYKEvAAYEnscgyWQvGb7yvAQ5z/YBYatnAjakHRDO5kXtAB
AiEAoQm2tcP3IiBm8BxstWKJlJ3xYA1euqLFdPnAaPQ5L6ECIQCCCYE0CSeLxgWw
YqJyStqFbAzlUO1yarWIL3L61IeL6QIgenKQYxVmzKQmoVx7rFAInOCbsJV5+h/a
VF+zVhqdgQECIEZZ3gzI5xw3hdxngHtVA+QrEM7/eXbtREjpYstRMAQBAiAy3g7Q
ikw16ABtUnL1IVcwxBPZpSowDd5G3bcJyt+NSQ==
-----END RSA PRIVATE KEY-----""";

        @Override
        String getKeyId() {
            return null
        }

        @Override
        InputStream getPrivateKey() {
            return new ByteArrayInputStream(DUMMY_PEM_KEY.getBytes())
        }

        @Override
        String getPassPhrase() {
            return null
        }

        @Override
        char[] getPassphraseCharacters() {
            return new char[0]
        }
    }
}
