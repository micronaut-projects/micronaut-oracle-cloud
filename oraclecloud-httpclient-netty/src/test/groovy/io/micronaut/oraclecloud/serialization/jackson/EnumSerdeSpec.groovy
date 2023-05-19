package io.micronaut.oraclecloud.serialization.jackson


import io.micronaut.oraclecloud.serialization.jackson.model.TestStateEnum
import io.micronaut.runtime.server.EmbeddedServer

class EnumSerdeSpec extends SerdeSpecBase {

    void "test enum serialization"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        expect:
        echoTest(embeddedServer, TestStateEnum.Active) == '"active"'
        echoTest(embeddedServer, TestStateEnum.Inactive) == '"inactive"'
        echoTest(embeddedServer, TestStateEnum.Deleted) == '"deleted"'
        echoTest(embeddedServer, TestStateEnum.UnknownEnumValue) == 'null'
    }

    void "test enum deserializatioin"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        expect:
        echoTest(embeddedServer, '"active"', TestStateEnum) == TestStateEnum.Active
        echoTest(embeddedServer, '"inactive"', TestStateEnum) == TestStateEnum.Inactive
        echoTest(embeddedServer, '"deleted"', TestStateEnum) == TestStateEnum.Deleted
        echoTest(embeddedServer, '"unknown value"', TestStateEnum) == TestStateEnum.UnknownEnumValue
        // Seems to be a wrong result
        echoTest(embeddedServer, 'null', TestStateEnum) == null
    }

}
