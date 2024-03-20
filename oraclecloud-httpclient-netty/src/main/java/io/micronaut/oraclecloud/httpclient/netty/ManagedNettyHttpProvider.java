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
package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.Serializer;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.HttpClientRegistry;
import io.micronaut.json.JsonMapper;
import io.micronaut.oraclecloud.serde.OciSdkMicronautSerializer;
import io.micronaut.oraclecloud.serde.OciSerdeConfiguration;
import io.micronaut.oraclecloud.serde.OciSerializationConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;

/**
 * {@link HttpProvider} implementation similar to {@link NettyHttpProvider}, but managed by an
 * {@link io.micronaut.context.ApplicationContext}.
 *
 * @since 3.0.0
 * @author Jonas Konrad
 */
@Singleton
@Internal
public class ManagedNettyHttpProvider implements HttpProvider {
    static final String SERVICE_ID = "oci";

    final HttpClientRegistry<?> mnHttpClientRegistry;
    final HttpClient mnHttpClient;
    final ExecutorService ioExecutor;
    final JsonMapper jsonMapper;

    @Inject
    public ManagedNettyHttpProvider(
        HttpClientRegistry<?> mnHttpClientRegistry,
        @Named(TaskExecutors.BLOCKING) ExecutorService ioExecutor,
        ObjectMapper jsonMapper,
        OciSerdeConfiguration ociSerdeConfiguration,
        OciSerializationConfiguration ociSerializationConfiguration
    ) {
        this.mnHttpClientRegistry = mnHttpClientRegistry;
        this.mnHttpClient = null;
        this.ioExecutor = ioExecutor;
        this.jsonMapper = jsonMapper.cloneWithConfiguration(ociSerdeConfiguration, ociSerializationConfiguration, null);
    }

    // for OKE
    public ManagedNettyHttpProvider(
        HttpClient mnHttpClient,
        ExecutorService ioExecutor
    ) {
        this.mnHttpClientRegistry = null;
        this.mnHttpClient = mnHttpClient;
        this.ioExecutor = ioExecutor;
        this.jsonMapper = OciSdkMicronautSerializer.getDefaultObjectMapper();
    }

    @Override
    public HttpClientBuilder newBuilder() {
        return new NettyHttpClientBuilder(this);
    }

    @Override
    public Serializer getSerializer() {
        return new OciSdkMicronautSerializer(jsonMapper);
    }
}
