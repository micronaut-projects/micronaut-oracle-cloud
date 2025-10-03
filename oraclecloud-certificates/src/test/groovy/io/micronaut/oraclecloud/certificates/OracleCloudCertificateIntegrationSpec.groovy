package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.certificates.model.CertificateBundle
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey
import com.oracle.bmc.certificates.model.Validity
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name="micronaut.server.ssl.enabled", value = "false")
@Property(name="test.certificates.server", value = "OracleCloudCertificateIntegrationSpec")
@Property(name="micronaut.security.enabled", value = "false")
class OracleCloudCertificateIntegrationSpec extends Specification {

    @Inject
    EmbeddedServer embeddedServer

    def "test HTTPS call"() {
        given:
        def props = [
                "micronaut.server.ssl.enabled": true,
                "oci.client.ssl.insecure-trust-all-certificates": "true",
                "micronaut.server.ssl.port": "0",
                "micronaut.http.client.ssl.insecure-trust-all-certificates": "true",
                "micronaut.ssl.enabled": "true",
                "micronaut.server.ssl.key-name": "server",
                "oci.certificates.server.certificate-id": "testCertId",
                "micronaut.security.enabled": "false",
                "test.client.url": embeddedServer.URI.toString()
        ]
        def ctx = ApplicationContext.builder().properties(props).start()
        def server = ctx.getBean(EmbeddedServer)
        server.start()
        HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL());
        BlockingHttpClient blocking = client.toBlocking()
        when:
        HttpResponse<String> response = blocking.exchange(
                HttpRequest.GET("/test"),
                String
        )

        then:
        response.body() == "OK"
    }

    def "test HTTPS call old api"() {
        given:
        def props = [
                "micronaut.server.ssl.enabled": true,
                "oci.client.ssl.insecure-trust-all-certificates": "true",
                "micronaut.server.ssl.port": "0",
                "micronaut.http.client.ssl.insecure-trust-all-certificates": "true",
                "micronaut.ssl.enabled": "true",
                "oci.certificates.certificate-id": "testCertId",
                "oci.certificates.enabled": "true",
                "micronaut.security.enabled": "false",
                "test.client.url": embeddedServer.URI.toString()
        ]
        def ctx = ApplicationContext.builder().properties(props).start()
        def server = ctx.getBean(EmbeddedServer)
        server.start()
        HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL());
        BlockingHttpClient blocking = client.toBlocking()
        when:
        HttpResponse<String> response = blocking.exchange(
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

    @Requires(property = "test.certificates.server", value = "OracleCloudCertificateIntegrationSpec")
    @Controller("/20210224")
    static class CertController {

        @Get("/certificateBundles/{certId}")
        CertificateBundle getCertificateBundleResponse(@PathVariable("certId") String certId) {
            return CertificateBundleWithPrivateKey.builder()
                    .certificateId(certId)
                    .timeCreated(new Date())
                    .privateKeyPem(OracleCloudServiceSpec.PRIVATE_KEY)
                    .serialNumber("test")
                    .privateKeyPemPassphrase(OracleCloudServiceSpec.PRIVATE_KEY_PASSPHRASE)
                    .certChainPem(OracleCloudServiceSpec.CERTIFICATE_CHAIN_STRING)
                    .certificatePem(OracleCloudServiceSpec.CERTIFICATE_STRING)
                    .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                    .build()
        }
    }
}
