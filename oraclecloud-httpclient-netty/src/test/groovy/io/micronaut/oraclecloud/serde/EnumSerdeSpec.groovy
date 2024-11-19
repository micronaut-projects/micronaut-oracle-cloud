package io.micronaut.oraclecloud.serde


import io.micronaut.oraclecloud.serde.model.TestStateEnum

class EnumSerdeSpec extends SerdeSpecBase {

    void "test enum serialization"() {
        expect:
        serialize(TestStateEnum.Active) == '"active"'
        serialize(TestStateEnum.Inactive) == '"inactive"'
        serialize(TestStateEnum.Deleted) == '"deleted"'
        serialize(TestStateEnum.UnknownEnumValue) == 'null'
    }

    void "test enum deserialization"() {
        expect:
        deserialize('"active"', TestStateEnum) == TestStateEnum.Active
        deserialize('"inactive"', TestStateEnum) == TestStateEnum.Inactive
        deserialize('"deleted"', TestStateEnum) == TestStateEnum.Deleted
        deserialize('null', TestStateEnum) == TestStateEnum.UnknownEnumValue
        deserialize('"unknown value"', TestStateEnum) == TestStateEnum.UnknownEnumValue
    }

}
