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

import com.oracle.bmc.http.client.HttpResponse;
import io.micronaut.core.annotation.Internal;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Internal
final class ApacheCoreHttpResponse implements HttpResponse {
    private final ApacheCoreHttpClient client;
    private final Closeable channel;
    private final ClassicHttpResponse response;

    ApacheCoreHttpResponse(ApacheCoreHttpClient client, Closeable channel, ClassicHttpResponse response) {
        this.client = client;
        this.channel = channel;
        this.response = response;
    }

    @Override
    public int status() {
        return response.getCode();
    }

    @Override
    public String header(String name) {
        Header header = response.getFirstHeader(name);
        return header == null ? null : header.getValue();
    }

    @Override
    public Map<String, List<String>> headers() {
        return Arrays.stream(response.getHeaders())
            .collect(Collectors.toMap(
                NameValuePair::getName,
                h -> List.of(h.getValue()),
                (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).toList())
            );
    }

    private InputStream stream() throws IOException {
        HttpEntity entity = response.getEntity();
        return entity == null ? new ByteArrayInputStream(new byte[0]) : entity.getContent();
    }

    @Override
    public CompletionStage<InputStream> streamBody() {
        try {
            return CompletableFuture.completedFuture(stream());
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public <T> CompletionStage<T> body(Class<T> type) {
        try {
            if (response.getEntity() == null || response.getEntity().getContentLength() == 0) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(client.provider.getSerializer().readValue(stream(), type));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public <T> CompletionStage<List<T>> listBody(Class<T> type) {
        try {
            return CompletableFuture.completedFuture(client.provider.getSerializer().readList(stream(), type));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletionStage<String> textBody() {
        try {
            return CompletableFuture.completedFuture(new String(stream().readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void close() {
        try {
            response.close();
            channel.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
