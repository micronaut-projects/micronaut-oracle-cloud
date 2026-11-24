/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.oraclecloud.function.client;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsInvokeAsyncClient;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link OciFunctionInvokeClients} that builds one client per invoke endpoint from the configured
 * {@link FunctionsInvokeClient.Builder} and {@link FunctionsInvokeAsyncClient.Builder} beans, so the created
 * clients share the configuration, client configurators, HTTP provider and authentication of the default
 * OCI Functions invoke clients.
 *
 * @since 6.2.5
 */
@Internal
@Singleton
@Requires(beans = AbstractAuthenticationDetailsProvider.class)
final class DefaultOciFunctionInvokeClients implements OciFunctionInvokeClients, AutoCloseable {
    private final FunctionsInvokeClient.Builder syncClientBuilder;
    private final FunctionsInvokeAsyncClient.Builder asyncClientBuilder;
    private final AbstractAuthenticationDetailsProvider authenticationDetailsProvider;
    private final Map<String, FunctionsInvokeClient> syncClients = new ConcurrentHashMap<>();
    private final Map<String, FunctionsInvokeAsyncClient> asyncClients = new ConcurrentHashMap<>();

    /**
     * @param syncClientBuilder The builder of the default synchronous invoke client
     * @param asyncClientBuilder The builder of the default asynchronous invoke client
     * @param authenticationDetailsProvider The authentication details provider
     */
    DefaultOciFunctionInvokeClients(
        FunctionsInvokeClient.Builder syncClientBuilder,
        FunctionsInvokeAsyncClient.Builder asyncClientBuilder,
        AbstractAuthenticationDetailsProvider authenticationDetailsProvider
    ) {
        this.syncClientBuilder = syncClientBuilder;
        this.asyncClientBuilder = asyncClientBuilder;
        this.authenticationDetailsProvider = authenticationDetailsProvider;
    }

    @Override
    public FunctionsInvokeClient syncClient(String invokeEndpoint) {
        return syncClients.computeIfAbsent(invokeEndpoint, endpoint -> {
            // copy the shared builder so that its region and endpoint are never mutated
            FunctionsInvokeClient client = FunctionsInvokeClient.builder()
                .copyFrom(syncClientBuilder)
                .build(authenticationDetailsProvider);
            // set after building: a region copied from the builder would otherwise override the endpoint
            client.setEndpoint(endpoint);
            return client;
        });
    }

    @Override
    public FunctionsInvokeAsyncClient asyncClient(String invokeEndpoint) {
        return asyncClients.computeIfAbsent(invokeEndpoint, endpoint -> {
            FunctionsInvokeAsyncClient client = FunctionsInvokeAsyncClient.builder()
                .copyFrom(asyncClientBuilder)
                .build(authenticationDetailsProvider);
            client.setEndpoint(endpoint);
            return client;
        });
    }

    @PreDestroy
    @Override
    public void close() {
        syncClients.values().forEach(FunctionsInvokeClient::close);
        syncClients.clear();
        asyncClients.values().forEach(FunctionsInvokeAsyncClient::close);
        asyncClients.clear();
    }
}
