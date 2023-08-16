package io.micronaut.oraclecloud.serde

import com.oracle.bmc.auth.internal.GetResourcePrincipalSessionTokenRequest
import com.oracle.bmc.auth.internal.JWK
import com.oracle.bmc.auth.internal.X509FederationClient
import com.oracle.bmc.model.RegionSchema
import io.micronaut.runtime.server.EmbeddedServer

class AdditionalModelsSerdeSpec extends SerdeSpecBase {

    void "test additional models are serdeable"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        expect:
        json == echoTest(embeddedServer, value)

        when:
        echoTest(embeddedServer, json, value.getClass())

        then:
        noExceptionThrown()

        where:
        value | json
        new RegionSchema("key", "comp", "region key", "identifier")
              | '{"realmKey":"key","realmDomainComponent":"comp","regionKey":"region key","regionIdentifier":"identifier"}'
        new GetResourcePrincipalSessionTokenRequest("token", "session token", "pk")
              | '{"resourcePrincipalToken":"token","servicePrincipalSessionToken":"session token","sessionPublicKey":"pk"}'
        new X509FederationClient.X509FederationRequest("key", "cert", [] as Set, "purple", "alg")
              | '{"intermediateCertificates":[],"certificate":"cert","publicKey":"key","purpose":"purple","fingerprintAlgorithm":"alg"}'
        new X509FederationClient.SecurityToken("my token")
              | '{"token":"my token"}'
        new JWK("some id", "some n", "some e")
              | '{"kty":"RSA","use":"sig","alg":"RS256","kid":"some id","n":"some n","e":"some e"}'
    }

}
