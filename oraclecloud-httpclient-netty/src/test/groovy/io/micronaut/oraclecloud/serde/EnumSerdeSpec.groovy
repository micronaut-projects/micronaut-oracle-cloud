package io.micronaut.oraclecloud.serde


import io.micronaut.oraclecloud.serde.model.TestStateEnum
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
        echoTest(embeddedServer, 'null', TestStateEnum) == TestStateEnum.UnknownEnumValue
        echoTest(embeddedServer, '"unknown value"', TestStateEnum) == TestStateEnum.UnknownEnumValue
    }

}
