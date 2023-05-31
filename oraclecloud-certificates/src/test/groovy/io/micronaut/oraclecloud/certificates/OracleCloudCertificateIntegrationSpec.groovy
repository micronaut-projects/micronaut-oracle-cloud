package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.certificates.Certificates
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey
import com.oracle.bmc.certificates.model.Validity
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name="oci.certificates.enabled", value = "true")
@Property(name="micronaut.server.dual-protocol", value = "true")
@Property(name="micronaut.server.ssl.enabled", value = "true")
@Property(name="oci.certificates.certificate-id", value = "testCertId")
@Property(name="micronaut.server.ssl.port", value = "8443")
@Property(name="micronaut.http.client.ssl.insecure-trust-all-certificates", value = "true")
class OracleCloudCertificateIntegrationSpec extends Specification {

    @Inject
    EmbeddedServer embeddedServer

    def "task test https"() {
        given:
        def asyncClient = embeddedServer.applicationContext.createBean(HttpClient, embeddedServer.getURL())
        BlockingHttpClient client = asyncClient.toBlocking()

        when:
        HttpResponse<String> response = client.exchange(
                HttpRequest.GET("/test"),
                String
        )

        then:
        response.body() == "OK"
    }

    @Controller("/test")
    @Produces(MediaType.TEXT_PLAIN)
    static class TestController {

        @Get("/")
        String ok() {
            return "OK"
        }
    }

    @MockBean(Certificates)
    @Primary
    @Context
    Certificates certificates() {
        def mockCertificates = Mock(Certificates)
        mockCertificates.getCertificateBundle(*_) >> GetCertificateBundleResponse.builder()
                .certificateBundle(
                        CertificateBundleWithPrivateKey.builder()
                                .privateKeyPem(OracleCloudServiceSpec.PRIVATE_KEY)
                                .certificateId("testId")
                                .serialNumber("test")
                                .timeCreated(new Date())
                                .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                                .certificatePem(OracleCloudServiceSpec.CERTIFICATE_STRING).build())
                .build()
        return mockCertificates
    }
}
