/*
 * Copyright 2017-2022 original authors
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

import com.oracle.bmc.http.client.ClientProperty;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.RequestInterceptor;
import com.oracle.bmc.http.client.StandardClientProperties;
import io.micronaut.core.annotation.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class NettyHttpClientBuilder implements HttpClientBuilder {
    final Collection<PrioritizedValue<RequestInterceptor>> requestInterceptors = new ArrayList<>();
    @Nullable
    ManagedNettyHttpProvider managedProvider;
    final Map<ClientProperty<?>, Object> properties = new HashMap<>();
    URI baseUri;
    boolean buffered = true;
    String serviceId = ManagedNettyHttpProvider.SERVICE_ID;

    NettyHttpClientBuilder(@Nullable ManagedNettyHttpProvider managedProvider) {
        this.managedProvider = managedProvider;
    }

    @Override
    public HttpClientBuilder baseUri(URI uri) {
        this.baseUri = Objects.requireNonNull(uri, "baseUri");
        return this;
    }

    @Override
    public HttpClientBuilder baseUri(String uri) {
        this.baseUri = URI.create(Objects.requireNonNull(uri, "baseUri"));
        return this;
    }

    @Override
    public <T> HttpClientBuilder property(ClientProperty<T> key, T value) {
        if (key == StandardClientProperties.READ_TIMEOUT ||
            key == StandardClientProperties.CONNECT_TIMEOUT ||
            key == StandardClientProperties.ASYNC_POOL_SIZE) {
            properties.put(key, value);
        } else if (key == StandardClientProperties.BUFFER_REQUEST) {
            buffered = (Boolean) value;
        } else if (key == NettyClientProperties.SERVICE_ID) {
            if (managedProvider == null) {
                throw new IllegalArgumentException("Can only configure the service ID for the managed netty http client");
            } else if (managedProvider.mnHttpClientRegistry == null) {
                throw new IllegalArgumentException("Cannot configure the service ID when the client is passed explicitly");
            }
            serviceId = (String) value;
        } else if (key == StandardClientProperties.KEY_STORE ||
            key == StandardClientProperties.TRUST_STORE ||
            key == StandardClientProperties.HOSTNAME_VERIFIER ||
            key == StandardClientProperties.SSL_CONTEXT) {
            throw new IllegalArgumentException("The OCI SDK netty client does not support changing the this setting (" + key + ") directly. Please go through the Micronaut HTTP client configuration.");
        } else if (key == NettyClientProperties.MANAGED_PROVIDER) {
            if (managedProvider == null) {
                managedProvider = (ManagedNettyHttpProvider) value;
            }
        } else {
            // todo: support all standard client properties
            throw new IllegalArgumentException("Unknown or unsupported HTTP client property " + key);
        }
        return this;
    }

    @Override
    public HttpClientBuilder registerRequestInterceptor(int priority, RequestInterceptor interceptor) {
        Objects.requireNonNull(interceptor, "interceptor");
        requestInterceptors.add(new PrioritizedValue<>(priority, interceptor));
        return this;
    }

    @Override
    public HttpClient build() {
        return new NettyHttpClient(this);
    }

    static final class PrioritizedValue<T> {
        final int priority;
        final T value;

        PrioritizedValue(int priority, T value) {
            this.priority = priority;
            this.value = value;
        }
    }
}
