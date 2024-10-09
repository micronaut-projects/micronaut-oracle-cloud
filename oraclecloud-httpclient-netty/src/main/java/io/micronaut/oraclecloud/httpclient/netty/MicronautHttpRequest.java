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

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import io.micronaut.buffer.netty.NettyByteBufferFactory;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.body.AvailableByteBody;
import io.micronaut.http.body.ByteBody;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.http.body.stream.InputStreamByteBody;
import io.micronaut.http.netty.body.AvailableNettyByteBody;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import static io.micronaut.oraclecloud.httpclient.netty.NettyClientProperties.CLASS_AND_METHOD_KEY_NAME;

final class MicronautHttpRequest implements HttpRequest {

    private static final long UNKNOWN_CONTENT_LENGTH = -1;

    private final NettyHttpClient client;

    private final Map<String, Object> attributes;

    @Nullable
    private MutableHttpRequest<?> mnRequest = null;

    private final StringBuilder uri;
    private final StringBuilder query;

    private Executor offloadExecutor;
    private Thread blockHint;

    private boolean expectContinue;
    private Object returningBody;
    @Nullable
    private CloseableByteBody byteBody;

    public MicronautHttpRequest(NettyHttpClient nettyHttpClient, Method method) {
        client = nettyHttpClient;
        this.uri = new StringBuilder(client.baseUri.toString());
        attributes = new HashMap<>();
        StackWalker.StackFrame frame = StackWalker.getInstance().walk(s -> s.filter(stackFrame -> stackFrame.getClassName().contains("com.oracle.bmc") && !stackFrame.getClassName().contains("com.oracle.bmc.http.internal")).toList()).stream().findFirst().orElse(null);
        attributes.put(CLASS_AND_METHOD_KEY_NAME, frame == null ? "N/A" : Arrays.stream(frame.getClassName().split("\\.")).reduce((first, second) -> second).orElse("N/A") + "." + frame.getMethodName());
        query = new StringBuilder();
        mnRequest = io.micronaut.http.HttpRequest.create(switch (method) {
            case GET -> HttpMethod.GET;
            case HEAD -> HttpMethod.HEAD;
            case DELETE -> HttpMethod.DELETE;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case PATCH -> HttpMethod.PATCH;
        }, "");
    }

    private MicronautHttpRequest(MicronautHttpRequest from) {
        this.client = from.client;
        this.attributes = new HashMap<>(from.attributes);
        this.mnRequest = from.mnRequest == null ? null : copyRequest(from.mnRequest);
        this.uri = new StringBuilder(from.uri);
        this.query = new StringBuilder(from.query);
        this.offloadExecutor = from.offloadExecutor;
        this.blockHint = from.blockHint;
        this.expectContinue = from.expectContinue;

        this.returningBody = from.returningBody;
        this.byteBody = from.byteBody == null ? null : from.byteBody.split(ByteBody.SplitBackpressureMode.FASTEST); // todo
    }

    private static MutableHttpRequest<?> copyRequest(io.micronaut.http.HttpRequest<?> original) {
        MutableHttpRequest<Object> req = io.micronaut.http.HttpRequest.create(original.getMethod(), original.getUri().toString());
        for (Map.Entry<String, List<String>> entry : original.getHeaders()) {
            for (String value : entry.getValue()) {
                req.getHeaders().add(entry.getKey(), value);
            }
        }
        return req;
    }

    @Override
    public Method method() {
        if (mnRequest == null) {
            return null;
        }
        return switch (mnRequest.getMethod()) {
            case GET -> Method.GET;
            case HEAD -> Method.HEAD;
            case POST -> Method.POST;
            case PUT -> Method.PUT;
            case DELETE -> Method.DELETE;
            case PATCH -> Method.PATCH;
            default -> throw new UnsupportedOperationException("Unsupported method: " + mnRequest.getMethodName());
        };
    }

    @Override
    public HttpRequest body(Object body) {
        if (byteBody != null) {
            byteBody.close();
        }

        if (body instanceof String) {
            byteBody = new AvailableNettyByteBody(ByteBufUtil.encodeString(client.alloc(), CharBuffer.wrap((CharSequence) body), StandardCharsets.UTF_8));
            returningBody = body;
        } else if (body instanceof InputStream) {
            body((InputStream) body, UNKNOWN_CONTENT_LENGTH);
        } else if (body == null) {
            byteBody = AvailableNettyByteBody.empty();
            returningBody = "";
        } else {
            // todo: would be better to write directly to ByteBuf here, but RequestSignerImpl does not yet support
            //  anything but String
            String json;
            try {
                json = client.jsonMapper.writeValueAsString(body);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to process JSON body", e);
            }
            byteBody = new AvailableNettyByteBody(ByteBufUtil.encodeString(client.alloc(), CharBuffer.wrap(json), StandardCharsets.UTF_8));
            returningBody = json;
        }
        return this;
    }

    @Override
    public HttpRequest body(InputStream body, long contentLength) {
        byteBody = InputStreamByteBody.create(
            body,
            contentLength == UNKNOWN_CONTENT_LENGTH ? OptionalLong.empty() : OptionalLong.of(contentLength),
            client.blockingIoExecutor,
            NettyByteBufferFactory.DEFAULT
        );
        returningBody = body;
        return this;
    }

    @Override
    public Object body() {
        return returningBody;
    }

    @Override
    public HttpRequest appendPathPart(String encodedPathPart) {
        boolean hasSlashLeft = uri.charAt(uri.length() - 1) == '/';
        boolean hasSlashRight = encodedPathPart.startsWith("/");
        if (hasSlashLeft) {
            if (hasSlashRight) {
                uri.append(encodedPathPart, 1, encodedPathPart.length());
            } else {
                uri.append(encodedPathPart);
            }
        } else {
            if (hasSlashRight) {
                uri.append(encodedPathPart);
            } else {
                uri.append('/').append(encodedPathPart);
            }
        }
        return this;
    }

    @Override
    public HttpRequest query(String name, String value) {
        if (!query.isEmpty()) {
            query.append('&');
        }
        query.append(name).append('=').append(value);
        return this;
    }

    private String buildUri() {
        int length = uri.length();
        if (!query.isEmpty()) {
            uri.append('?').append(query);
        }
        String built = uri.toString();
        uri.setLength(length); // remove query again
        return built;
    }

    @Override
    public URI uri() {
        return URI.create(buildUri());
    }

    @Override
    public HttpRequest header(String name, String value) {
        if (mnRequest == null) {
            mnRequest = io.micronaut.http.HttpRequest.POST("", null); // placeholder
        }
        mnRequest.header(name, value);
        if (HttpHeaderNames.EXPECT.contentEqualsIgnoreCase(name)) {
            expectContinue = HttpHeaderValues.CONTINUE.contentEqualsIgnoreCase(value);
        }
        return this;
    }

    @Override
    public Map<String, List<String>> headers() {
        return new MicronautHeaderMap(mnRequest.getHeaders());
    }

    @Override
    public Object attribute(String name) {
        return attributes.get(name);
    }

    @Override
    public HttpRequest removeAttribute(String name) {
        attributes.remove(name);
        return this;
    }

    @Override
    public HttpRequest attribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    @Override
    public HttpRequest offloadExecutor(Executor offloadExecutor) {
        this.offloadExecutor = offloadExecutor;
        // this is technically not what this setter is for, but offloadExecutor() is only called in
        // callSync and always at top level, i.e. in the thread that will actually block.
        this.blockHint = Thread.currentThread();
        return this;
    }

    @Override
    public HttpRequest copy() {
        return new MicronautHttpRequest(this);
    }

    @Override
    public void discard() {
        if (byteBody != null) {
            byteBody.close();
        }
    }

    @Override
    public CompletionStage<HttpResponse> execute() {
        // jersey client buffers even when BUFFER_REQUEST is off, if the content length is not explicitly set.
        if (byteBody != null && !(byteBody instanceof AvailableByteBody) && (client.buffered || byteBody.expectedLength().isEmpty()) && !expectContinue) {

            // asynchronously buffer the body, then run execute() again
            return byteBody.buffer()
                .thenCompose(v -> {
                    this.byteBody = v;
                    return execute();
                });
        }

        for (RequestInterceptor interceptor : client.requestInterceptors) {
            interceptor.intercept(this);
        }

        finalizeRequest();

        List<Object> filterState = new ArrayList<>(client.nettyClientFilter.size());
        for (OciNettyClientFilter<?> filter : client.nettyClientFilter) {
            filterState.add(filter.beforeRequest(this));
        }

        return Mono.from(client.upstreamHttpClient.exchange(mnRequest, byteBody, blockHint))
            .toFuture()
            .thenApply(r -> (HttpResponse) new MicronautHttpResponse(client.jsonMapper, r, offloadExecutor))
            .exceptionallyCompose(e -> runResponseFilters(filterState, null, e))
            .thenCompose(r -> runResponseFilters(filterState, r, null));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private CompletableFuture<HttpResponse> runResponseFilters(List<Object> filterState, HttpResponse response, Throwable exception) {
        if (exception instanceof CompletionException && exception.getCause() != null) {
            exception = exception.getCause();
        }

        for (int i = client.nettyClientFilter.size() - 1; i >= 0; i--) {
            try {
                ((OciNettyClientFilter) client.nettyClientFilter.get(i))
                    .afterResponse(this, response, exception, filterState.get(i));
            } catch (Exception e) {
                if (exception == null) {
                    response.close();
                    response = null;
                } else {
                    e.addSuppressed(exception);
                }
                exception = e;
            }
        }
        if (exception != null) {
            return CompletableFuture.failedFuture(exception);
        } else {
            return CompletableFuture.completedFuture(response);
        }
    }

    private void finalizeRequest() {
        String uriString = buildUri();

        URI uri = URI.create(uriString);
        mnRequest.uri(uri);
        if (!mnRequest.getHeaders().contains(HttpHeaders.HOST)) {
            mnRequest.getHeaders().add(HttpHeaderNames.HOST, uri.getHost());
        }

        if (!mnRequest.getHeaders().contains(HttpHeaders.CONTENT_LENGTH) && !mnRequest.getHeaders().contains(HttpHeaders.TRANSFER_ENCODING)) {
            // the RawHttpClient would set these headers, but they need to be visible from filters
            OptionalLong contentLength = byteBody == null ? OptionalLong.of(0) : byteBody.expectedLength();
            if (contentLength.isPresent()) {
                mnRequest.getHeaders().add(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength.getAsLong()));
            } else {
                mnRequest.getHeaders().add(HttpHeaders.TRANSFER_ENCODING, "chunked");
            }
        }
    }
}
