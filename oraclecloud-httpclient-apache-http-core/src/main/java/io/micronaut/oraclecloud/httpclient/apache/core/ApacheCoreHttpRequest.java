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

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HeaderElements;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.impl.io.ChunkedInputStream;
import org.apache.hc.core5.http.impl.io.ChunkedOutputStream;
import org.apache.hc.core5.http.impl.io.ContentLengthInputStream;
import org.apache.hc.core5.http.impl.io.ContentLengthOutputStream;
import org.apache.hc.core5.http.impl.io.DefaultClassicHttpRequestFactory;
import org.apache.hc.core5.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.hc.core5.http.impl.io.DefaultHttpResponseParserFactory;
import org.apache.hc.core5.http.impl.io.SessionInputBufferImpl;
import org.apache.hc.core5.http.impl.io.SessionOutputBufferImpl;
import org.apache.hc.core5.http.io.HttpMessageParser;
import org.apache.hc.core5.http.io.SessionOutputBuffer;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

final class ApacheCoreHttpRequest implements HttpRequest {
    private final ApacheCoreHttpClient client;

    private final Method method;
    private final StringBuilder uri;
    private final StringBuilder query;
    private final Map<String, List<String>> headers;
    private final Map<String, Object> attributes;
    private HttpEntity entity;

    ApacheCoreHttpRequest(ApacheCoreHttpClient client, Method method) {
        this.client = client;
        this.method = method;
        this.uri = new StringBuilder(client.baseUri.toString());
        this.query = new StringBuilder();
        this.headers = new HashMap<>();
        this.attributes = new HashMap<>();
        this.entity = null;
    }

    private ApacheCoreHttpRequest(ApacheCoreHttpRequest prototype) {
        this.client = prototype.client;
        this.method = prototype.method;
        this.uri = prototype.uri;
        this.query = prototype.query;
        // deep copy
        this.headers = prototype.headers.entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey(), new ArrayList<>(e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.attributes = new HashMap<>(prototype.attributes);
        this.entity = prototype.entity;
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public HttpRequest body(Object body) {
        if (body instanceof String s) {
            entity = HttpEntities.create(s);
        } else if (body instanceof InputStream is) {
            body(is, -1);
        } else if (body == null) {
            entity = null;
        } else {
            entity = HttpEntities.create(os -> client.provider.getSerializer().writeValue(os, body), null);
        }
        return this;
    }

    @Override
    public HttpRequest body(InputStream body, long contentLength) {
        entity = new InputStreamEntity(body, contentLength, null);
        return this;
    }

    @Override
    public Object body() {
        throw new UnsupportedOperationException("Request body access is not supported. Are you trying to use the request signer?");
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
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers;
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
    public HttpRequest offloadExecutor(Executor executor) {
        return this;
    }

    @Override
    public HttpRequest copy() {
        return new ApacheCoreHttpRequest(this);
    }

    @Override
    public void discard() {
        // there's always a request that tried writing the entity
    }

    @Override
    public CompletionStage<HttpResponse> execute() {
        for (RequestInterceptor requestInterceptor : client.requestInterceptors) {
            requestInterceptor.intercept(this);
        }

        ClassicHttpRequest request = DefaultClassicHttpRequestFactory.INSTANCE
            .newHttpRequest(method.name(), buildUri());
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                request.addHeader(entry.getKey(), value);
            }
        }

        SocketChannel channel = null;
        Exception ex = null;
        try {
            fillContentLength(request);

            channel = SocketChannel.open(UnixDomainSocketAddress.of(client.socketPath));

            // write request headers
            OutputStream os = Channels.newOutputStream(channel);
            SessionOutputBuffer outputBuffer = new SessionOutputBufferImpl(Http1Config.DEFAULT.getBufferSize());
            DefaultHttpRequestWriterFactory.INSTANCE.create().write(request, outputBuffer, os);

            // write request body
            boolean expectContinue = isExpectContinue(request);
            if (!expectContinue) {
                writeEntity(request, outputBuffer, os);
            } else {
                outputBuffer.flush(os);
            }

            // read response
            InputStream is = Channels.newInputStream(channel);
            SessionInputBufferImpl inBuffer = new SessionInputBufferImpl(Http1Config.DEFAULT.getBufferSize());
            HttpMessageParser<ClassicHttpResponse> parser = DefaultHttpResponseParserFactory.INSTANCE.create(Http1Config.DEFAULT);
            ClassicHttpResponse classicHttpResponse = parser.parse(inBuffer, is);

            // if we are told to continue, write the body and read the next response
            if (expectContinue && classicHttpResponse.getCode() == HttpStatus.SC_CONTINUE) {
                writeEntity(request, outputBuffer, os);
                classicHttpResponse = parser.parse(inBuffer, is);
            }

            readAndSetResponseEntity(classicHttpResponse, inBuffer, is);

            ApacheCoreHttpResponse response = new ApacheCoreHttpResponse(client, channel, classicHttpResponse);
            channel = null; // response will close the channel
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            ex = e;
            return CompletableFuture.failedFuture(e);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ioe) {
                    if (ex != null) {
                        ex.addSuppressed(ioe);
                    }
                }
            }
        }
    }

    private static boolean isExpectContinue(ClassicHttpRequest request) {
        Header expect = request.getFirstHeader(HttpHeaders.EXPECT);
        return expect != null && expect.getValue().equalsIgnoreCase(HeaderElements.CONTINUE);
    }

    private void fillContentLength(ClassicHttpRequest request) throws IOException {
        if (!request.containsHeader(HttpHeaders.CONTENT_LENGTH) && !request.containsHeader(HttpHeaders.TRANSFER_ENCODING)) {
            if (entity == null) {
                request.addHeader(HttpHeaders.CONTENT_LENGTH, 0);
            } else if (entity.getContentLength() >= 0) {
                request.addHeader(HttpHeaders.CONTENT_LENGTH, entity.getContentLength());
            } else {
                if (client.buffered || !(isExpectContinue(request) && entity.getContentLength() == -1)) {
                    byte[] bytes = entity.getContent().readAllBytes();
                    entity = HttpEntities.create(bytes, null);
                    request.addHeader(HttpHeaders.CONTENT_LENGTH, bytes.length);
                } else {
                    request.addHeader(HttpHeaders.TRANSFER_ENCODING, HeaderElements.CHUNKED_ENCODING);
                }
            }
        }
    }

    private static void readAndSetResponseEntity(ClassicHttpResponse classicHttpResponse, SessionInputBufferImpl inBuffer, InputStream is) {
        Header contentLength = classicHttpResponse.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            long n = Long.parseLong(contentLength.getValue());
            classicHttpResponse.setEntity(new InputStreamEntity(new ContentLengthInputStream(inBuffer, is, n), n, null));
        } else {
            Header transferEncoding = classicHttpResponse.getFirstHeader(HttpHeaders.TRANSFER_ENCODING);
            if (transferEncoding != null && transferEncoding.getValue().equalsIgnoreCase(HeaderElements.CHUNKED_ENCODING)) {
                classicHttpResponse.setEntity(new InputStreamEntity(new ChunkedInputStream(inBuffer, is), null));
            }
        }
    }

    private void writeEntity(ClassicHttpRequest request, SessionOutputBuffer buffer, OutputStream rawStream) throws IOException {
        Header contentLength = request.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
        OutputStream bodyStream;
        if (contentLength != null) {
            long len = Long.parseLong(contentLength.getValue());
            bodyStream = new ContentLengthOutputStream(buffer, rawStream, len);
        } else {
            bodyStream = new ChunkedOutputStream(buffer, rawStream, 0);
        }
        try (bodyStream) {
            if (entity != null) {
                entity.writeTo(bodyStream);
            }
        }
    }
}
