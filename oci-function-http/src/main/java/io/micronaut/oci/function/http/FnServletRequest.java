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
package io.micronaut.oci.function.http;

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.cookie.Cookies;
import io.micronaut.servlet.http.ServletCookies;
import io.micronaut.servlet.http.ServletHttpRequest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Implementation of {@link ServletHttpRequest} for Project.fn.
 *
 * @author graemerocher
 * @since 1.0.0
 * @param <B> The body type
 */
@Internal
final class FnServletRequest<B> implements ServletHttpRequest<InputEvent, B> {

    private final InputEvent inputEvent;
    private final HTTPGatewayContext gatewayContext;
    private MutableConvertibleValues<Object> attributes;
    private ServletCookies cookies;

    public FnServletRequest(
            InputEvent inputEvent,
            HTTPGatewayContext gatewayContext) {
        this.inputEvent = inputEvent;
        this.gatewayContext = gatewayContext;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException("Calling getInputStream() is not supported. If you need an InputSteam define a parameter of type InputEvent and use the consumeBody method");
    }

    @Override
    public BufferedReader getReader() {
        throw new UnsupportedOperationException("Calling getReader() is not supported. If you need an InputSteam define a parameter of type InputEvent and use the consumeBody method");
    }

    @NonNull
    @Override
    public <T> Optional<T> getBody(@NonNull Argument<T> type) {
        return inputEvent.consumeBody(inputStream -> ConversionService.SHARED.convert(inputStream, type));
    }

    @Override
    public InputEvent getNativeRequest() {
        return inputEvent;
    }

    @NonNull
    @Override
    public Cookies getCookies() {
        ServletCookies cookies = this.cookies;
        if (cookies == null) {
            synchronized (this) { // double check
                cookies = this.cookies;
                if (cookies == null) {
                    cookies = new ServletCookies(getPath(), getHeaders(), ConversionService.SHARED);
                    this.cookies = cookies;
                }
            }
        }
        return cookies;
    }

    @NonNull
    @Override
    public HttpParameters getParameters() {
        return new FnHttpParameters();
    }

    @NonNull
    @Override
    public HttpMethod getMethod() {
        return HttpMethod.valueOf(gatewayContext.getMethod());
    }

    @NonNull
    @Override
    public String getMethodName() {
        return gatewayContext.getMethod();
    }

    @NonNull
    @Override
    public URI getUri() {
        return URI.create(gatewayContext.getRequestURL());
    }

    @NonNull
    @Override
    public HttpHeaders getHeaders() {
        return new FnHttpHeaders();
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
        return Optional.empty();
    }

    /**
     * The fn parameters.
     */
    private final class FnHttpParameters implements HttpParameters {

        @Override
        public List<String> getAll(CharSequence name) {
            if (name != null) {
                return gatewayContext.getQueryParameters().getValues(name.toString());
            }
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public String get(CharSequence name) {
            if (name != null) {
                return gatewayContext.getQueryParameters()
                            .get(name.toString())
                            .orElse(null);
            }
            return null;
        }

        @Override
        public Set<String> names() {
            return gatewayContext.getQueryParameters().getAll().keySet();
        }

        @Override
        public Collection<List<String>> values() {
            return gatewayContext.getQueryParameters().getAll().values();
        }

        @Override
        public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
            if (name != null) {
                Optional<String> v = gatewayContext.getQueryParameters().get(name.toString());
                return v.flatMap(s -> ConversionService.SHARED.convert(
                        s, conversionContext
                ));
            }
            return Optional.empty();
        }
    }

    /**
     * The fn headers.
     */
    private final class FnHttpHeaders implements HttpHeaders {

        @Override
        public List<String> getAll(CharSequence name) {
            if (name != null) {
                return gatewayContext.getHeaders().getAllValues(name.toString());
            }
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public String get(CharSequence name) {
            if (name != null) {
                return gatewayContext.getHeaders().get(name.toString()).orElse(null);
            }
            return null;
        }

        @Override
        public Set<String> names() {
            return new HashSet<>(gatewayContext.getHeaders().keys());
        }

        @Override
        public Collection<List<String>> values() {
            return gatewayContext.getHeaders().asMap().values();
        }

        @Override
        public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
            if (name != null) {
                Optional<String> v = gatewayContext.getHeaders().get(name.toString());
                return v.flatMap(s -> ConversionService.SHARED.convert(
                    s, conversionContext
                ));
            }
            return Optional.empty();
        }
    }
}
