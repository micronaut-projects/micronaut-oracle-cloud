/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oraclecloud.function.http;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.netty.cookies.NettyCookie;
import io.micronaut.servlet.http.ServletHttpResponse;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the {@link ServletHttpResponse} interface for Project.fn.
 *
 * @author graemerocher
 * @since 1.0.0
 * @param <B> The body type
 */
@Internal
final class FnServletResponse<B> implements ServletHttpResponse<OutputEvent, B> {
    private final Map<String, List<String>> headers = new LinkedHashMap<>(10);
    private final HTTPGatewayContext gatewayContext;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private HttpStatus status = HttpStatus.OK;
    private MutableConvertibleValues<Object> attributes;
    private B bodyObject;

    FnServletResponse(HTTPGatewayContext gatewayContext) {
        this.gatewayContext = gatewayContext;
    }

    @Override
    public OutputEvent getNativeResponse() {
        return OutputEvent.fromBytes(
                body.toByteArray(),
                status.getCode() <= 499 ? OutputEvent.Status.Success : OutputEvent.Status.FunctionError,
                getContentType().orElse(MediaType.APPLICATION_JSON_TYPE).toString(),
                toFnHeaders()
        );
    }

    private Headers toFnHeaders() {
        Map<String, List<String>> fnHeaders = new LinkedHashMap<>(headers.size());
        headers.forEach((name, values) -> fnHeaders.put("Fn-Http-H-" + name, values));
        return Headers.fromMultiHeaderMap(fnHeaders);
    }

    @Override
    public OutputStream getOutputStream() {
        return body;
    }

    @Override
    public BufferedWriter getWriter() {
        return new BufferedWriter(new OutputStreamWriter(body, getCharacterEncoding()));
    }

    @Override
    public MutableHttpResponse<B> cookie(Cookie cookie) {
        if (cookie instanceof NettyCookie) {
            NettyCookie nettyCookie = (NettyCookie) cookie;
            final String encoded = ServerCookieEncoder.STRICT.encode(nettyCookie.getNettyCookie());
            header(HttpHeaders.SET_COOKIE, encoded);
        }
        return this;
    }

    @Override
    public MutableHttpHeaders getHeaders() {
        return new FnResponseHeaders();
    }

    @NonNull
    @Override
    public MutableConvertibleValues<Object> getAttributes() {
        MutableConvertibleValues<Object> attributes = this.attributes;
        if (attributes == null) {
            synchronized (this) { // double check
                attributes = this.attributes;
                if (attributes == null) {
                    attributes = new MutableConvertibleValuesMap<>();
                    this.attributes = attributes;
                }
            }
        }
        return attributes;
    }

    @NonNull
    @Override
    public Optional<B> getBody() {
        return Optional.ofNullable(bodyObject);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> MutableHttpResponse<T> body(@Nullable T body) {
        this.bodyObject = (B) body;
        return (MutableHttpResponse<T>) this;
    }

    @Override
    public MutableHttpResponse<B> status(HttpStatus status, CharSequence message) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.gatewayContext.setStatusCode(status.getCode());
        return this;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Models the response headers for project fn.
     */
    private final class FnResponseHeaders extends FnMultiValueMap implements MutableHttpHeaders {

        /**
         * Default constructor.
         */
        FnResponseHeaders() {
            super(headers);
        }

        @Override
        public MutableHttpHeaders add(CharSequence header, CharSequence value) {
            if (header != null && value != null) {
                headers.computeIfAbsent(header.toString(), s -> new ArrayList<>(5)).add(value.toString());
            }
            return this;
        }

        @Override
        public MutableHttpHeaders remove(CharSequence header) {
            if (header != null) {
                headers.remove(header.toString());
            }
            return this;
        }
    }
}
