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
import com.oracle.bmc.http.client.KeyStoreWithPassword;
import com.oracle.bmc.http.client.RequestInterceptor;
import com.oracle.bmc.http.client.StandardClientProperties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

final class NettyHttpClientBuilder implements HttpClientBuilder {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    final Collection<PrioritizedValue<RequestInterceptor>> requestInterceptors = new ArrayList<>();

    URI baseUri;
    Duration connectTimeout = DEFAULT_TIMEOUT;
    Duration readTimeout = DEFAULT_TIMEOUT;
    int asyncPoolSize = 0;
    boolean buffered = true;

    KeyStoreWithPassword keyStore;
    KeyStore trustStore;
    HostnameVerifier hostnameVerifier;
    SSLContext sslContext;

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
        if (key == StandardClientProperties.CONNECT_TIMEOUT) {
            connectTimeout = (Duration) value;
        } else if (key == StandardClientProperties.READ_TIMEOUT) {
            readTimeout = (Duration) value;
        } else if (key == StandardClientProperties.ASYNC_POOL_SIZE) {
            asyncPoolSize = (Integer) value;
        } else if (key == StandardClientProperties.BUFFER_REQUEST) {
            buffered = (Boolean) value;
        } else if (key == StandardClientProperties.KEY_STORE) {
            keyStore = (KeyStoreWithPassword) value;
        } else if (key == StandardClientProperties.TRUST_STORE) {
            trustStore = (KeyStore) value;
        } else if (key == StandardClientProperties.HOSTNAME_VERIFIER) {
            hostnameVerifier = (HostnameVerifier) value;
        } else if (key == StandardClientProperties.SSL_CONTEXT) {
            sslContext = (SSLContext) value;
        } else {
            // todo: support all standard client properties
            throw new IllegalArgumentException(
                    "Unknown or unsupported HTTP client property " + key);
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
