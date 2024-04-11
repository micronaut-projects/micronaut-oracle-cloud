package io.micronaut.oraclecloud.serde

import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider
import com.oracle.bmc.http.client.HttpProvider
import com.oracle.bmc.http.client.Serializer


class AuthSerdeSpec extends SerdeSpecBase {

    // We cannot change the session token authentication details provider's endpoint
    // so we just verify that we can serialize the needed beans.
    void "session token authentication request serialization test"() throws IOException {
        given:

        Serializer serializer = HttpProvider.getDefault().getSerializer()

        when:
        String request = serializer.writeValueAsString(
                new SessionTokenAuthenticationDetailsProvider.SessionTokenRefreshRequest.SessionTokenRequest("my-token"))

        then:
        "{\"currentToken\":\"my-token\"}" == request
    }

    void "session token authentication response deserialization test"() throws IOException {
        given:
        Serializer serializer = HttpProvider.getDefault().getSerializer()

        when:
        SessionTokenAuthenticationDetailsProvider.SessionToken response =
                serializer.readValue("{\"token\":\"new-token\"}", SessionTokenAuthenticationDetailsProvider.SessionToken.class)

        then:
        "new-token" == response.getToken()
    }

}
