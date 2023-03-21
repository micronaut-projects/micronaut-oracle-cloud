package io.micronaut.oraclecloud.serde.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = BaseModel.class)
@JsonSubTypes(
    @JsonSubTypes.Type(value = ComplexModel.class, name = "complex")
)
@JsonFilter("explicitlySetFilter")
@JsonDeserialize(builder = BaseModel.Builder.class)
public class BaseModel extends ExplicitlySetBmcModel {
    @JsonProperty("baseString")
    protected String baseString;

    @JsonProperty("baseInt")
    protected Integer baseInteger;

    public BaseModel(String baseString, Integer baseInteger) {
        this.baseString = baseString;
        this.baseInteger = baseInteger;
    }

    public String getBaseString() {
        return baseString;
    }

    public Integer getBaseInteger() {
        return baseInteger;
    }

    public boolean equals(Object o) {
        return equals(o, true);
    }

    public boolean equals(Object o, boolean explicitlySet) {
        if (!(o instanceof BaseModel)) {
            return false;
        }
        BaseModel other = (BaseModel) o;
        return Objects.equals(other.baseString, baseString) &&
            Objects.equals(other.baseInteger, baseInteger) &&
            (!explicitlySet || super.equals(o));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toString() {
        return "{" +
            "baseString=" + (baseString != null || wasPropertyExplicitlySet("baseString") ? baseString : "") + "," +
            "baseInteger=" + (baseInteger != null || wasPropertyExplicitlySet("baseInt") ? baseInteger : "") +
        "}";
    }

    protected static class Builder {
        protected String baseString;
        protected Integer baseInteger;

        protected final Set<String> explicitlySet = new HashSet<>();

        public Builder baseString(String baseString) {
            this.baseString = baseString;
            explicitlySet.add("baseString");
            return this;
        }

        public Builder baseInteger(Integer baseInteger) {
            this.baseInteger = baseInteger;
            explicitlySet.add("baseInt");
            return this;
        }

        public BaseModel build() {
            BaseModel model = new BaseModel(baseString, baseInteger);
            explicitlySet.forEach(model::markPropertyAsExplicitlySet);
            return model;
        }

        public String toString() {
            return build().toString();
        }
    }
}
