package io.micronaut.oraclecloud.serde


import com.oracle.bmc.http.internal.ResponseHelper
import io.micronaut.runtime.server.EmbeddedServer

class ErrorResponseSerdeSpec extends SerdeSpecBase {

    void "ErrorCodeAndMessage deserialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var response = echoTest(embeddedServer, body, ResponseHelper.ErrorCodeAndMessage)

        then:
        builder.build() == response

        where:
        body    | builder
        '{"code":"Unauthorized","message":"Access not allowed"}'
                | ResponseHelper.ErrorCodeAndMessage.builder().code("Unauthorized").message("Access not allowed")
        '{"code":null}'
                | ResponseHelper.ErrorCodeAndMessage.builder().code(null)
        '{"code":"Unauthorized","originalMessage":"message","originalMessageTemplate":"template"}'
                | ResponseHelper.ErrorCodeAndMessage.builder().code("Unauthorized").originalMessage("message").originalMessageTemplate("template")
        '{"code":"Unauthorized","messageArguments":{"arg":"val"}}'
                | ResponseHelper.ErrorCodeAndMessage.builder().code("Unauthorized").messageArguments(["arg": "val"])
    }

}
