package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.certificates.model.CertificateBundle
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey
import com.oracle.bmc.certificates.model.Validity
import io.micronaut.context.annotation.Property
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.ssl.OracleCloudServiceSpec
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.x509.X509Authentication
import io.micronaut.context.annotation.Requires
import io.micronaut.context.ApplicationContext

import static io.micronaut.http.MediaType.TEXT_PLAIN
import static io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS


@MicronautTest
@Property(name="micronaut.server.ssl.enabled", value = "false")
@Property(name="test.certificates.server", value = "OracleCloudCertificateMTLSSpec")
@Property(name="micronaut.security.enabled", value = "false")
class OracleCloudCertificateMTLSSpec extends Specification {

    @Inject
    EmbeddedServer embeddedServer

    def  "mtls handshake should succeed with OCI-provided client cert"() {
        given:
        def props = [
                "micronaut.server.ssl.enabled": true,
                "micronaut.server.ssl.port": "0",
                "oci.certificates.server.certificate-id": "testClientCertId",
                "oci.certificates.client.certificate-id": "testClientCertId2",
                "micronaut.server.ssl.client-authentication": "NEED",
                "micronaut.ssl.enabled": "true",
                "micronaut.security.x509.enabled": "true",
                "micronaut.server.ssl.key-name": "server",
                "micronaut.server.ssl.trust-name": "server",
                "oci.client.ssl.trust-name": "client",
                "oci.client.ssl.key-name": "client",
                "micronaut.http.client.ssl.trust-name": "client",
                "micronaut.http.client.ssl.key-name": "client",
                "test.client.url": embeddedServer.URI.toString()
        ]
        def ctx = ApplicationContext.builder().properties(props).start()
        def server = ctx.getBean(EmbeddedServer)
        server.start()
        HttpClient client = server.getApplicationContext().createBean(HttpClient.class, server.getURL())
        BlockingHttpClient blocking = client.toBlocking()

        when:
        def response = blocking.exchange(HttpRequest.GET("/mtls"), String)

        then:
        response.body().contains("CN=micronaut.guide.x509")  // cert subject inspected in controller
    }

    @Controller("/mtls")
    static class MtlsController {

        @Secured(IS_ANONYMOUS)
        @Get(produces = TEXT_PLAIN)
        String hello(@Nullable X509Authentication x509Auth,
                     @Nullable Authentication authentication) {
            if (!x509Auth && !authentication) {
                return "Hello unknown!";
            }
            if (!x509Auth) {
                return "ERROR: Authentication is present but not X509Authentication";
            }
            if (!x509Auth.is(authentication)) {
                return "ERROR: Authentication and X509Authentication should be the same instance";
            }
            "Hello ${x509Auth.name} (X.509 cert issued by ${x509Auth.certificate.issuerX500Principal.name})";
        }
    }

    @Requires(property = "test.certificates.server", value = "OracleCloudCertificateMTLSSpec")
    @Controller("/20210224")
    static class CertController {

        @Get("/certificateBundles/{certId}")
        CertificateBundle getCertificateBundleResponse(@PathVariable("certId") String certId) {
            if (certId == "testClientCertId") {
                return CertificateBundleWithPrivateKey.builder()
                        .certificateId(certId)
                        .timeCreated(new Date())
                        .privateKeyPem(OracleCloudServiceSpec.PRIVATE_KEY)
                        .certChainPem(OracleCloudServiceSpec.CERTIFICATE_CHAIN_STRING)
                        .privateKeyPemPassphrase(OracleCloudServiceSpec.PRIVATE_KEY_PASSPHRASE)
                        .serialNumber("test")
                        .certificatePem(OracleCloudServiceSpec.CERTIFICATE_STRING)
                        .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                        .build()
            }

            return CertificateBundleWithPrivateKey.builder()
                    .certificateId(certId)
                    .timeCreated(new Date())
                    .privateKeyPem(OracleCloudServiceSpec.CLIENT_PRIVATE_KEY)
                    .certChainPem(OracleCloudServiceSpec.CERTIFICATE_CHAIN_STRING)
                    .serialNumber("test")
                    .certificatePem(OracleCloudServiceSpec.CLIENT_CERTIFICATE_STRING)
                    .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                    .build()
        }
    }
}
