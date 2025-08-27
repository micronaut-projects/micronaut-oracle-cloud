package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.certificates.Certificates
import com.oracle.bmc.certificates.model.CertificateBundleWithPrivateKey
import com.oracle.bmc.certificates.model.Validity
import com.oracle.bmc.certificates.responses.GetCertificateBundleResponse
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.oraclecloud.certificates.ssl.OracleCloudClientSSLContextBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.x509.X509AuthenticationFetcher
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.x509.X509Authentication


import static io.micronaut.http.MediaType.TEXT_PLAIN
import static io.micronaut.security.rules.SecurityRule.IS_ANONYMOUS


@MicronautTest
@Property(name="oci.certificates.enabled", value = "true")
@Property(name="micronaut.server.dual-protocol", value = "true")
@Property(name="micronaut.server.ssl.enabled", value = "true")
@Property(name="oci.certificates.server.certificate-id", value = "testServerCertId")
@Property(name="oci.certificates.clients.default.certificate-id", value = "testClientCertId")
@Property(name="micronaut.server.ssl.port", value = "8443")
@Property(name="micronaut.server.ssl.client-authentication", value="NEED")
@Property(name="oci.client.ssl.insecure-trust-all-certificates", value = "true")
@Property(name="micronaut.ssl.enabled", value = "true")
@Property(name = "micronaut.security.x509.enabled", value = "true")
class OracleCloudCertificateMTLSSpec extends Specification {

    @Inject
    EmbeddedServer embeddedServer

    @Inject
    @Client("/")
    HttpClient client

    @Inject
    OracleCloudClientSSLContextBuilder oracleCloudClientSSLContextBuilder

    def  "mtls handshake should succeed with OCI-provided client cert"() {
        given:
        BlockingHttpClient client = client.toBlocking()

        when:
        def response = client.exchange(HttpRequest.GET("/mtls"), String)

        then:
        response.body().contains("CN=TestClient")  // cert subject inspected in controller
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

    @MockBean(Certificates)
    @Primary
    @Context
    Certificates certificates() {
        def mock = Mock(Certificates)
        // mock server cert
        mock.getCertificateBundle({ it.certificateId == "testServerCertId" }) >> GetCertificateBundleResponse.builder()
                .certificateBundle(CertificateBundleWithPrivateKey.builder()
                        .privateKeyPem(OracleCloudServiceSpec.PRIVATE_KEY)
                        .certificateId("testServerCertId")
                        .serialNumber("server")
                        .timeCreated(new Date())
                        .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                        .certificatePem(OracleCloudServiceSpec.CERTIFICATE_STRING).build())
                .build()

        // mock client cert (this is what your OracleCloudClientSSLContextBuilder will pick up)
        mock.getCertificateBundle({ ("testClientCertId" == it.certificateId) }) >> GetCertificateBundleResponse.builder()
                .certificateBundle(CertificateBundleWithPrivateKey.builder()
                        .privateKeyPem(OracleCloudServiceSpec.PRIVATE_KEY)
                        .certificateId("testClientCertId")
                        .serialNumber("client")
                        .timeCreated(new Date())
                        .validity(Validity.builder().timeOfValidityNotBefore(new Date()).timeOfValidityNotAfter(new Date()).build())
                        .certificatePem(OracleCloudServiceSpec.CERTIFICATE_STRING).build())
                .build()

        return mock
    }
}
