/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.serde.filter;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;
import com.oracle.bmc.http.client.internal.ExplicitlySetFilter;
import io.micronaut.serde.PropertyFilter;
import io.micronaut.serde.Serializer;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * An implementation of property filter that chooses all explicitly set properties for
 * serialization. It uses the {@link ExplicitlySetBmcModel#wasPropertyExplicitlySet} method to
 * retrieve the information from a model. OCI SDK models should be created with a builder for
 * the functionality to work correctly.<br/>
 *
 * All the non-null properties are also kept, as those must have been explicitly set.
 * Additionally, this allows the serialization of the
 * {@link com.fasterxml.jackson.annotation.JsonTypeInfo#property} field, which is required for
 * serialization with inheritance.
 */
@Singleton
@Named(ExplicitlySetFilter.NAME)
public class ExplicitlySetPropertyFilter implements PropertyFilter {

    @Override
    public boolean shouldInclude(Serializer.EncoderContext encoderContext, Serializer<Object> propertySerializer, Object bean, String propertyName, Object propertyValue) {
        if (bean instanceof ExplicitlySetBmcModel) {
            // Non-null properties are always serialized
            if (propertyValue != null) {
                return true;
            }
            ExplicitlySetBmcModel model = (ExplicitlySetBmcModel) bean;
            return model.wasPropertyExplicitlySet(propertyName);
        }
        return false;
    }
}
