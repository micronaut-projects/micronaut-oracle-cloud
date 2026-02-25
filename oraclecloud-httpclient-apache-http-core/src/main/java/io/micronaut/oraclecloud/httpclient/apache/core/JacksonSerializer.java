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
import com.oracle.bmc.encryption.internal.EncryptionHeader;
import com.oracle.bmc.encryption.internal.EncryptionKey;
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.annotation.Internal;
import io.micronaut.jackson.Jackson2AnnotationSupport;
import io.micronaut.jackson.databind.JacksonDatabindMapper;
import jakarta.inject.Singleton;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.util.StdDateFormat;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Internal
@Singleton
@Bean(typed = ApacheCoreSerializer.class)
@Requires(classes = {ObjectMapper.class})
@Requires(property = "spec.name", notEquals = "ManagedSerdeNettyTest")
@BootstrapContextCompatible
@Secondary
final class JacksonSerializer extends ApacheCoreSerializer {
    private static final ObjectMapper MAPPER;

    static {
        JsonMapper.Builder builder = JsonMapper.builder()
            .addModule(new MyModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .defaultDateFormat(new Rfc3339DateFormat())
            .filterProvider(new SimpleFilterProvider().addFilter("explicitlySetFilter", ExplicitlySetFilter.INSTANCE));
        Jackson2AnnotationSupport.installJackson2Introspector(builder);
        MAPPER = builder.build();
    }

    JacksonSerializer() {
        super(new JacksonDatabindMapper(MAPPER));
    }

    @SuppressWarnings({"MethodDoesntCallSuperMethod"})
    private static final class Rfc3339DateFormat extends StdDateFormat {
        // from java-sdk

        private static final DateTimeFormatter ISO8601_WITH_MILLIS = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX", Locale.US)
            .withZone(ZoneId.of("UTC"));

        Rfc3339DateFormat() {
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, java.text.FieldPosition fieldPosition) {
            // Same as ISO8601DateFormat but we always serialize millis
            toAppendTo.append(ISO8601_WITH_MILLIS.format(date.toInstant()));
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
        public void serializeAsProperty(
            Object pojo, JsonGenerator jgen, SerializationContext provider, PropertyWriter writer)
            throws Exception {

            if (include(writer)) {
                Field field = getMatchingDeclaredField(pojo.getClass(), writer.getName());
                boolean accessible = field.isAccessible();
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(pojo);
                    if (fieldValue != null) {
                        // not null, definitely serialize
                        writer.serializeAsProperty(pojo, jgen, provider);
                    } else if (pojo instanceof ExplicitlySetBmcModel) {
                        // null, find out if null was explicitly set using the
                        //      method from BmcModel common class
                        if (((ExplicitlySetBmcModel) pojo).wasPropertyExplicitlySet(writer.getName())) {
                            writer.serializeAsProperty(pojo, jgen, provider);
                        }
                    } else if (hasExplicitlySetInAField(pojo, writer)) {
                        // To be removed on the next architecture-level change
                        //      kept for compatibility reasons
                        // null, find out if model has explicitlySet property
                        writer.serializeAsProperty(pojo, jgen, provider);
                    }
                } finally {
                    field.setAccessible(accessible);
                }
            } else if (!jgen.canOmitProperties()) {
                writer.serializeAsOmittedProperty(pojo, jgen, provider);
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

    private static final class MyModule extends SimpleModule {
        {
            addDeserializer(EncryptionHeader.class, new EncryptionHeaderDeserializer());
        }
    }

    private static final class EncryptionHeaderDeserializer extends ValueDeserializer<EncryptionHeader> {
        @Override
        public EncryptionHeader deserialize(tools.jackson.core.JsonParser p, tools.jackson.databind.DeserializationContext ctxt) throws tools.jackson.core.JacksonException {
            EncryptionHeaderDto dto = p.readValueAs(EncryptionHeaderDto.class);
            EncryptionHeader result = new EncryptionHeader();
            for (EncryptionKey key : dto.encryptedDataKeys) {
                result.setEncryptionHeader(key, dto.IV, dto.additionalAuthenticatedData);
            }
            return result;
        }

        record EncryptionHeaderDto(
            String additionalAuthenticatedData,
            String IV,
            List<EncryptionKey> encryptedDataKeys
        ) {
        }
    }
}
