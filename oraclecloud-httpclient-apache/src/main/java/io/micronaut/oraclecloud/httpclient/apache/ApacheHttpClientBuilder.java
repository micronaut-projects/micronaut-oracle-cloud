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

import com.oracle.bmc.http.client.ClientProperty;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.RequestInterceptor;
import com.oracle.bmc.http.client.StandardClientProperties;
import io.micronaut.core.annotation.Internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@Internal
final class ApacheHttpClientBuilder implements HttpClientBuilder {
    static final String SOCKET_PATH_PROPERTY = "io.micronaut.oraclecloud.httpclient.apache.socket-path";

    final ApacheHttpProvider provider;
    final Collection<PrioritizedInterceptor> requestInterceptors = new ArrayList<>();
    URI baseUri;
    boolean buffered = true;
    Path socketPath;

    ApacheHttpClientBuilder(ApacheHttpProvider provider) {
        this.provider = provider;
        String socketPathProperty = System.getProperty(SOCKET_PATH_PROPERTY);
        if (socketPathProperty != null) {
            socketPath = Path.of(socketPathProperty);
        }
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
        if (key == StandardClientProperties.BUFFER_REQUEST) {
            buffered = (Boolean) value;
        } else if (key == ApacheHttpProvider.SOCKET_PATH) {
            socketPath = (Path) value;
        } else {
            throw new IllegalArgumentException("Unknown or unsupported HTTP client property " + key);
        }
        return this;
    }

    @Override
    public HttpClientBuilder registerRequestInterceptor(int priority, RequestInterceptor interceptor) {
        Objects.requireNonNull(interceptor, "interceptor");
        requestInterceptors.add(new PrioritizedInterceptor(priority, interceptor));
        return this;
    }

    @Override
    public HttpClient build() {
        return new ApacheHttpClient(this);
    }

    record PrioritizedInterceptor(int priority, RequestInterceptor interceptor) {
    }
}
