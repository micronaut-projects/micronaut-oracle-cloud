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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.oraclecloud.httpclient.apache.core.serde.OciSerdeConfiguration;
import io.micronaut.oraclecloud.httpclient.apache.core.serde.OciSerializationConfiguration;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.config.annotation.SerdeConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
@Bean(typed = ApacheCoreSerializer.class)
@Requires(bean = ObjectMapper.class)
@Requires(property = "spec.name", notEquals = "ManagedJacksonNettyTest")
@BootstrapContextCompatible
final class SerdeSerializer extends ApacheCoreSerializer {
    @Inject
    SerdeSerializer(ObjectMapper objectMapper, OciSerdeConfiguration ociSerdeConfiguration, OciSerializationConfiguration ociSerializationConfiguration) {
        super(objectMapper.cloneWithConfiguration(ociSerdeConfiguration, ociSerializationConfiguration, null));
    }

    SerdeSerializer() {
        super(UnmanagedSerializerHolder.DEFAULT_MAPPER);
    }

    private static final class UnmanagedSerializerHolder {
        // only initialize if necessary

        private static final Map<String, Object> DEFAULT_MAPPER_CONFIG = Map.of(
            "micronaut.serde.writeDatesAsTimestamps", false,
            "micronaut.serde.write-binary-as-array", false,
            "micronaut.serde.serialization.inclusion", SerdeConfig.SerInclude.NON_NULL
        );

        private static final ObjectMapper DEFAULT_MAPPER = ObjectMapper.create(
            DEFAULT_MAPPER_CONFIG,
            "io.micronaut.oraclecloud.httpclient.apache.core.serde.filter",
            "io.micronaut.oraclecloud.httpclient.apache.core.serde.serializers"
        );
    }
}
