/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.oraclecloud.httpclient.apache.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Internal
@Singleton
@Bean(typed = ApacheCoreSerializer.class)
@Requires(classes = {ObjectMapper.class, JavaTimeModule.class})
@Requires(property = "spec.name", notEquals = "ManagedSerdeNettyTest")
@BootstrapContextCompatible
final class JacksonSerializer implements ApacheCoreSerializer {
    private final ObjectMapper objectMapper = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .defaultDateFormat(new Rfc3339DateFormat())
        .addModule(new JavaTimeModule())
        .filterProvider(new SimpleFilterProvider().addFilter("explicitlySetFilter", ExplicitlySetFilter.INSTANCE))
        .build();

    @Override
    public <T> T readValue(String s, Class<T> type) throws IOException {
        return objectMapper.readValue(s, type);
    }

    @Override
    public <T> T readValue(byte[] bytes, Class<T> type) throws IOException {
        return objectMapper.readValue(bytes, type);
    }

    @Override
    public String writeValueAsString(Object o) throws IOException {
        return objectMapper.writeValueAsString(o);
    }

    @Override
    public <T> T readValue(InputStream inputStream, Class<T> type) throws IOException {
        return objectMapper.readValue(inputStream, type);
    }

    @Override
    public <T> List<T> readList(InputStream inputStream, Class<T> type) throws IOException {
        return objectMapper.readValue(inputStream, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    @Override
    public void writeValue(OutputStream outputStream, Object value) throws IOException {
        objectMapper.writeValue(outputStream, value);
    }

    @SuppressWarnings({"deprecation", "MethodDoesntCallSuperMethod"})
    private static final class Rfc3339DateFormat extends StdDateFormat {
        // from java-sdk

        Rfc3339DateFormat() {
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
            // Same as ISO8601DateFormat but we always serialize millis
            toAppendTo.append(ISO8601Utils.format(date, true));
            return toAppendTo;
        }

        @Override
        public Rfc3339DateFormat clone() {
            return new Rfc3339DateFormat();
        }
    }

    @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
    private static final class ExplicitlySetFilter extends SimpleBeanPropertyFilter {
        // from java-sdk

        public static final ExplicitlySetFilter INSTANCE = new ExplicitlySetFilter();
        private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(ExplicitlySetFilter.class);

        ExplicitlySetFilter() {
        }

        @Override
        public void serializeAsField(
            Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
            throws Exception {

            if (include(writer)) {
                Field field = getMatchingDeclaredField(pojo.getClass(), writer.getName());
                boolean accessible = field.isAccessible();
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(pojo);
                    if (fieldValue != null) {
                        // not null, definitely serialize
                        writer.serializeAsField(pojo, jgen, provider);
                    } else if (pojo instanceof ExplicitlySetBmcModel) {
                        // null, find out if null was explicitly set using the
                        //      method from BmcModel common class
                        if (((ExplicitlySetBmcModel) pojo).wasPropertyExplicitlySet(writer.getName())) {
                            writer.serializeAsField(pojo, jgen, provider);
                        }
                    } else if (hasExplicitlySetInAField(pojo, writer)) {
                        // To be removed on the next architecture-level change
                        //      kept for compatibility reasons
                        // null, find out if model has explicitlySet property
                        writer.serializeAsField(pojo, jgen, provider);
                    }
                } finally {
                    field.setAccessible(accessible);
                }
            } else if (!jgen.canOmitFields()) {
                // since 2.3
                writer.serializeAsOmittedField(pojo, jgen, provider);
            }
        }

        @Deprecated
        @SuppressWarnings("unchecked")
        private boolean hasExplicitlySetInAField(Object pojo, PropertyWriter writer) throws Exception {
            Field explicitField = pojo.getClass().getDeclaredField(ExplicitlySetBmcModel.EXPLICITLY_SET_FILTER_NAME);
            boolean explicitAccessible = explicitField.isAccessible();
            try {
                explicitField.setAccessible(true);
                Set<String> explicitlySet = (Set<String>) explicitField.get(pojo);
                if (explicitlySet.contains(writer.getName())) {
                    return true;
                }
            } finally {
                explicitField.setAccessible(explicitAccessible);
            }

            return false;
        }

        private static Field getDeclaredField(Class<?> pojoClass, String fieldName)
            throws NoSuchFieldException {
            try {
                return pojoClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException nsfe) {
                Class<?> superclass = pojoClass.getSuperclass();
                if (superclass != null) {
                    return getDeclaredField(superclass, fieldName);
                } else {
                    throw nsfe;
                }
            }
        }

        private static Field getMatchingDeclaredField(Class<?> pojoClass, String fieldName)
            throws NoSuchFieldException {
            // Try matching the exact field name
            try {
                return getDeclaredField(pojoClass, fieldName);
            } catch (NoSuchFieldException nsfe) {
                LOG.debug("Exact field name match failed for {}", fieldName);
            }
            // If not found, try converting the field name from snake case to camel case
            String lowerCamelCased = lowerUnderscoreToLowerCamel(fieldName);
            try {
                return getDeclaredField(pojoClass, lowerCamelCased);
            } catch (NoSuchFieldException nsfe) {
                LOG.debug(
                    "Exact field name match failed for {}, lower camel-case {} didn't work either",
                    fieldName, lowerCamelCased);
                // Look through all fields and find a field with a matching JsonProperty annotation
                for (Field f : pojoClass.getDeclaredFields()) {
                    for (JsonProperty a : f.getAnnotationsByType(JsonProperty.class)) {
                        if (fieldName.equals(a.value())) {
                            return f;
                        }
                    }
                }
                throw nsfe;
            }
        }

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return include((PropertyWriter) writer);
        }

        @Override
        protected boolean include(PropertyWriter writer) {
            return !ExplicitlySetBmcModel.EXPLICITLY_SET_PROPERTY_NAME.equals(writer.getName());
        }

        private static String lowerUnderscoreToLowerCamel(String s) {
            StringBuilder sb = new StringBuilder(s);

            for (int i = 0; i < sb.length(); i++) {
                if (sb.charAt(i) == '_') {
                    sb.deleteCharAt(i);
                    sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
                }
            }
            return sb.toString();
        }
    }
}
