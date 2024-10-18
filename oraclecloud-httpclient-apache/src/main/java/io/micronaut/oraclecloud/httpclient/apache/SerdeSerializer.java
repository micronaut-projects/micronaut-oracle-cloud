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
package io.micronaut.oraclecloud.httpclient.apache;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.oraclecloud.httpclient.apache.serde.OciSerdeConfiguration;
import io.micronaut.oraclecloud.httpclient.apache.serde.OciSerializationConfiguration;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.config.annotation.SerdeConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Singleton
@Bean(typed = ApacheSerializer.class)
@Requires(bean = ObjectMapper.class)
@Requires(property = "spec.name", notEquals = "ManagedJacksonNettyTest")
@BootstrapContextCompatible
final class SerdeSerializer implements ApacheSerializer {
    private final ObjectMapper objectMapper;

    @Inject
    SerdeSerializer(ObjectMapper objectMapper, OciSerdeConfiguration ociSerdeConfiguration, OciSerializationConfiguration ociSerializationConfiguration) {
        this.objectMapper = objectMapper.cloneWithConfiguration(ociSerdeConfiguration, ociSerializationConfiguration, null);
    }

    SerdeSerializer() {
        this.objectMapper = UnmanagedSerializerHolder.DEFAULT_MAPPER;
    }

    @Override
    public <T> T readValue(InputStream inputStream, Class<T> type) throws IOException {
        return objectMapper.readValue(inputStream, type);
    }

    @Override
    public <T> List<T> readList(InputStream inputStream, Class<T> type) throws IOException {
        return objectMapper.readValue(inputStream, Argument.listOf(type));
    }

    @Override
    public void writeValue(OutputStream outputStream, Object value) throws IOException {
        objectMapper.writeValue(outputStream, value);
    }

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

    private static class UnmanagedSerializerHolder {
        // only initialize if necessary

        private static final Map<String, Object> DEFAULT_MAPPER_CONFIG = Map.of(
            "micronaut.serde.writeDatesAsTimestamps", false,
            "micronaut.serde.write-binary-as-array", false,
            "micronaut.serde.serialization.inclusion", SerdeConfig.SerInclude.NON_NULL
        );

        private static final ObjectMapper DEFAULT_MAPPER = ObjectMapper.create(
            DEFAULT_MAPPER_CONFIG,
            "io.micronaut.oraclecloud.httpclient.apache.serde.filter",
            "io.micronaut.oraclecloud.httpclient.apache.serde.serializers"
        );
    }
}
