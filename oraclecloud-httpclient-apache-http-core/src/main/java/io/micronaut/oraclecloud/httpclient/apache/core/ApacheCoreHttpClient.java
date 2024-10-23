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

import com.fasterxml.jackson.core.JacksonException;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import io.micronaut.core.annotation.Internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Internal
final class ApacheCoreHttpClient implements HttpClient {
    final ApacheCoreHttpProvider provider;
    final Path socketPath;
    final URI baseUri;
    final List<RequestInterceptor> requestInterceptors;
    final boolean buffered;

    ApacheCoreHttpClient(ApacheCoreHttpClientBuilder builder) {
        provider = builder.provider;
        socketPath = Objects.requireNonNull(builder.socketPath, "Please specify a socket path in the system property " + ApacheCoreHttpClientBuilder.SOCKET_PATH_PROPERTY + " or set it on the client using the ApacheHttpProvider.SOCKET_PATH property");
        requestInterceptors = builder.requestInterceptors.stream()
            .sorted(Comparator.comparingInt(ApacheCoreHttpClientBuilder.PrioritizedInterceptor::priority))
            .map(ApacheCoreHttpClientBuilder.PrioritizedInterceptor::interceptor)
            .collect(Collectors.toList());
        baseUri = Objects.requireNonNull(builder.baseUri, "baseUri");
        buffered = builder.buffered;
    }

    @Override
    public HttpRequest createRequest(Method method) {
        return new ApacheCoreHttpRequest(this, method);
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public boolean isProcessingException(Exception e) {
        return e instanceof JacksonException;
    }
}
