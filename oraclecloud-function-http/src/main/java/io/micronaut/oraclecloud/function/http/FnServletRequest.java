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

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.MediaType;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.http.cookie.Cookies;
import io.micronaut.http.simple.cookies.SimpleCookies;
import io.micronaut.servlet.http.ServletExchange;
import io.micronaut.servlet.http.ServletHttpRequest;
import io.micronaut.servlet.http.ServletHttpResponse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link ServletHttpRequest} for Project.fn.
 *
 * @author graemerocher
 * @since 1.0.0
 * @param <B> The body type
 */
@Internal
final class FnServletRequest<B> implements ServletHttpRequest<InputEvent, B>, ServletExchange<InputEvent, OutputEvent> {
    @SuppressWarnings("rawtypes")
    private static final Argument<ConvertibleValues> CONVERTIBLE_VALUES_ARGUMENT = Argument.of(ConvertibleValues.class);
    private final InputEvent inputEvent;
    private final HTTPGatewayContext gatewayContext;
    private final FnServletResponse<Object> response;

    private final ConversionService conversionService;
    private MutableConvertibleValues<Object> attributes;
    private Cookies cookies;
    private final MediaTypeCodecRegistry codecRegistry;
    private final Map<Argument, Optional> consumedBodies = new ConcurrentHashMap<>();
    private boolean bodyConsumed;

    public FnServletRequest(
        InputEvent inputEvent,
        FnServletResponse<Object> response,
        HTTPGatewayContext gatewayContext,
        ConversionService conversionService, MediaTypeCodecRegistry codecRegistry) {
        this.inputEvent = inputEvent;
        this.response = response;
        this.gatewayContext = gatewayContext;
        this.conversionService = conversionService;
        this.codecRegistry = codecRegistry;
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
    public <T> Optional<T> getBody(@NonNull Argument<T> arg) {
        if (arg == null) {
            return Optional.empty();
        }
        if (!bodyConsumed) {
            //noinspection unchecked
            return consumedBodies.computeIfAbsent(arg, argument -> inputEvent.consumeBody(inputStream -> {
                this.bodyConsumed = true;
                final Class<T> type = arg.getType();
                final MediaType contentType = getContentType().orElse(MediaType.APPLICATION_JSON_TYPE);

                final MediaTypeCodec codec = codecRegistry.findCodec(contentType, type).orElse(null);
                if (codec != null) {
                    if (ConvertibleValues.class == type || Object.class == type) {
                        final Map map = codec.decode(Map.class, inputStream);
                        ConvertibleValues result = ConvertibleValues.of(map);
                        return Optional.of(result);
                    } else {
                        final T value = codec.decode(arg, inputStream);
                        return Optional.ofNullable(value);
                    }

                }
                return Optional.empty();
            }));
        } else {
            Object body = consumedBodies.getOrDefault(CONVERTIBLE_VALUES_ARGUMENT, Optional.empty())
                        .orElse(null);
            if (body != null) {
                return consumedBodies.computeIfAbsent(arg, argument -> conversionService.convert(body, argument));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public InputEvent getNativeRequest() {
        return inputEvent;
    }

    @NonNull
    @Override
    public Cookies getCookies() {
        Cookies cookies = this.cookies;
        if (cookies == null) {
            synchronized (this) { // double check
                cookies = this.cookies;
                if (cookies == null) {
                    cookies = new SimpleCookies(conversionService);
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
        return (Optional<B>) getBody(CONVERTIBLE_VALUES_ARGUMENT);
    }

    @Override
    public ServletHttpRequest<InputEvent, ? super Object> getRequest() {
        //noinspection unchecked
        return (ServletHttpRequest) this;
    }

    @Override
    public ServletHttpResponse<OutputEvent, ? super Object> getResponse() {
        return response;
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
                return v.flatMap(s -> conversionService.convert(
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
                return v.flatMap(s -> conversionService.convert(
                    s, conversionContext
                ));
            }
            return Optional.empty();
        }
    }
}
