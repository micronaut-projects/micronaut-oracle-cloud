package io.micronaut.oraclecloud.serde.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oracle.bmc.http.internal.BmcEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TestStateEnum implements BmcEnum {
    Active("active"),
    Inactive("inactive"),
    Deleted("deleted"),
    UnknownEnumValue(null);

    private String value;

    private static final Map<String, TestStateEnum> valueMap = Arrays.stream(values())
        .collect(Collectors.toMap(v -> v.getValue(), Function.identity()));

    TestStateEnum(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TestStateEnum create(String value) {
        return valueMap.getOrDefault(value, UnknownEnumValue);
    }
}
