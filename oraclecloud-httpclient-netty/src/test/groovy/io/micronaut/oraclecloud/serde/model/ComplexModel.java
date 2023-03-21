package io.micronaut.oraclecloud.serde.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonFilter("explicitlySetFilter")
@JsonDeserialize(builder = ComplexModel.Builder.class)
public class ComplexModel extends BaseModel {
    @JsonProperty("string")
    protected String string;

    @JsonProperty("int")
    protected Integer integer;

    @JsonProperty("list")
    protected List<String> list;

    public String getString() {
        return string;
    }

    public Integer getInteger() {
        return integer;
    }

    public List<String> getList() {
        return list;
    }

    public ComplexModel(String baseString, Integer baseInteger, String string, Integer integer, List<String> list) {
        super(baseString, baseInteger);
        this.string = string;
        this.integer = integer;
        this.list = list;
    }

    public boolean equals(Object o, boolean explicitlySet) {
        if (!(o instanceof ComplexModel)) {
            return false;
        }
        ComplexModel other = (ComplexModel) o;
        return Objects.equals(other.string, string) &&
            Objects.equals(other.integer, integer) &&
            Objects.equals(other.list, list) &&
            super.equals(other, explicitlySet);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toString() {
        return "{" +
            "baseString=" + (baseString != null || wasPropertyExplicitlySet("baseString") ? baseString : "") + "," +
            "baseInt=" + (baseInteger != null || wasPropertyExplicitlySet("baseInt") ? baseInteger : "") + "," +
            "string=" + (string != null || wasPropertyExplicitlySet("string") ? string : "") + "," +
            "int=" + (integer != null || wasPropertyExplicitlySet("int") ? integer : "") + "," +
            "list=" + (list != null || wasPropertyExplicitlySet("list") ? list : "") +
        "}";
    }

    protected static class Builder extends BaseModel.Builder {
        private String string;
        private Integer integer;
        private List<String> list;

        public Builder string(String string) {
            this.string = string;
            explicitlySet.add("string");
            return this;
        }

        public Builder integer(Integer integer) {
            this.integer = integer;
            explicitlySet.add("int");
            return this;
        }

        public Builder list(List<String> list) {
            this.list = list;
            explicitlySet.add("list");
            return this;
        }

        public ComplexModel build() {
            ComplexModel model = new ComplexModel(baseString, baseInteger, string, integer, list);
            explicitlySet.forEach(model::markPropertyAsExplicitlySet);
            return model;
        }

        public String toString() {
            return build().toString();
        }
    }
}
