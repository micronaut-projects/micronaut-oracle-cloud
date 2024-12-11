package io.micronaut.oraclecloud.serde

import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider

class AuthSerdeSpec extends SerdeSpecBase {

    // We cannot change the session token authentication details provider's endpoint
    // so we just verify that we can serialize the needed beans.
    void "session token authentication request serialization test"() throws IOException {
        when:
        String request = serialize(
                new SessionTokenAuthenticationDetailsProvider.SessionTokenRefreshRequest.SessionTokenRequest("my-token"))

        then:
        "{\"currentToken\":\"my-token\"}" == request
    }

    void "session token authentication response deserialization test"() throws IOException {
        when:
        SessionTokenAuthenticationDetailsProvider.SessionToken response =
                deserialize("{\"token\":\"new-token\"}", SessionTokenAuthenticationDetailsProvider.SessionToken.class)

        then:
        "new-token" == response.getToken()
    }

}
