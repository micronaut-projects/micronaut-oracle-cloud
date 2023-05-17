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
package io.micronaut.oraclecloud.serde;

import com.oracle.bmc.http.client.Serializer;
import com.oracle.bmc.http.internal.ResponseHelper;
import com.oracle.bmc.model.RegionSchema;
import io.micronaut.core.annotation.Internal;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.SerdeImport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link Serializer} based on micronaut serde using {@link ObjectMapper}.
 * <br/>
 * Use {@link OciSdkMicronautSerializer#getDefaultSerializer()} method to get the default implementation
 * configured for use inside an Oracle Cloud SDK HTTP client.
 */
@Internal
@SerdeImport(ResponseHelper.ErrorCodeAndMessage.class)
@SerdeImport(RegionSchema.class)
public final class OciSdkMicronautSerializer implements Serializer {

    private static final Map<String, Object> DEFAULT_MAPPER_CONFIG = new HashMap<String, Object>() {{
        put("micronaut.serde.writeDatesAsTimestamps", false);
    }};

    private static final ObjectMapper DEFAULT_MAPPER = ObjectMapper.create(
        DEFAULT_MAPPER_CONFIG,
        "io.micronaut.oraclecloud.serde.filter",
        "io.micronaut.oraclecloud.serde.serializers"
    );

    private static final Serializer DEFAULT_SERIALIZER = new OciSdkMicronautSerializer(DEFAULT_MAPPER);

    private final ObjectMapper objectMapper;

    /**
     * Create Serializer from micronaut serde {@link ObjectMapper}
     * @param objectMapper the object mapper
     */
    public OciSdkMicronautSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T readValue(String s, Class<T> aClass) throws IOException {
        return objectMapper.readValue(s, aClass);
    }

    @Override
    public <T> T readValue(byte[] bytes, Class<T> aClass) throws IOException {
        return objectMapper.readValue(bytes, aClass);
    }

    @Override
    public String writeValueAsString(Object o) throws IOException {
        return objectMapper.writeValueAsString(o);
    }

    /**
     * @return The implementation of object mapper configured for oci java sdk
     */
    public static ObjectMapper getDefaultObjectMapper() {
        return DEFAULT_MAPPER;
    }

    /**
     * @return The implementation of serializer configured for oci java sdk
     */
    public static Serializer getDefaultSerializer() {
        return DEFAULT_SERIALIZER;
    }
}
