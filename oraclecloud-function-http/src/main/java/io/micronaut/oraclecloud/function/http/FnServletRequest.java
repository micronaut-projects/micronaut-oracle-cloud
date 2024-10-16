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
import io.micronaut.core.convert.value.ConvertibleMultiValues;
import io.micronaut.core.convert.value.ConvertibleMultiValuesMap;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpParameters;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.ServerHttpRequest;
import io.micronaut.http.body.ByteBody;
import io.micronaut.http.body.ByteBody.SplitBackpressureMode;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.Cookies;
import io.micronaut.http.netty.cookies.NettyCookie;
import io.micronaut.http.simple.cookies.SimpleCookies;
import io.micronaut.servlet.http.ServletExchange;
import io.micronaut.servlet.http.ServletHttpRequest;
import io.micronaut.servlet.http.ServletHttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ServletHttpRequest} for Project.fn.
 *
 * @author graemerocher
 * @since 1.0.0
 * @param <B> The body type
 */
@Internal
final class FnServletRequest<B> implements ServletHttpRequest<InputEvent, B>, ServletExchange<InputEvent, OutputEvent>, MutableHttpRequest<B>, ServerHttpRequest<B> {

    private static final String COOKIE_HEADER = "Cookie";

    @SuppressWarnings("rawtypes")
    static final Argument<ConvertibleValues> CONVERTIBLE_VALUES_ARGUMENT = Argument.of(ConvertibleValues.class);
    private final InputEvent inputEvent;
    private final HTTPGatewayContext gatewayContext;
    private final FnServletResponse<Object> response;

    private final ConversionService conversionService;
    private MutableConvertibleValues<Object> attributes;
    private Cookies cookies;
    private final MediaTypeCodecRegistry codecRegistry;
    private final ByteBody byteBody;
    private Object cachedBody;
    private URI uri;

    public FnServletRequest(
        ByteBody byteBody,
        InputEvent inputEvent,
        FnServletResponse<Object> response,
        HTTPGatewayContext gatewayContext,
        ConversionService conversionService,
        MediaTypeCodecRegistry codecRegistry
    ) {
        this.byteBody = byteBody;
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
        return byteBody.toInputStream();
    }

    /**
     * A method that allows consuming body of the {@link InputEvent}.
     *
     * @return The result
     * @param <T> The function return value
     */
    public <T> T consumeBody(Function<InputStream, T> consumer) {
        return consumer.apply(byteBody.split(SplitBackpressureMode.FASTEST).toInputStream());
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
    }

    @NonNull
    @Override
    public <T> Optional<T> getBody(@NonNull Argument<T> arg) {
        if (arg == null) {
            return Optional.empty();
        }
        final Class<T> type = arg.getType();
        final MediaType contentType = getContentType().orElse(MediaType.APPLICATION_JSON_TYPE);

        if (isFormSubmission()) {
            ConvertibleMultiValues<?> form;
            if (cachedBody instanceof ConvertibleMultiValues<?> storedForm) {
                form = storedForm;
            } else {
                try {
                    String content = IOUtils.readText(new BufferedReader(new InputStreamReader(byteBody.toInputStream(), getCharacterEncoding())));
                    form = parseFormData(content);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to parse body", e);
                }
                cachedBody = form;
            }

            if (ConvertibleValues.class == type || Object.class == type) {
                return Optional.of((T) form);
            } else {
                return conversionService.convert(form.asMap(), arg);
            }
        }

        final MediaTypeCodec codec = codecRegistry.findCodec(contentType, type).orElse(null);
        if (codec == null) {
            return Optional.empty();
        }
        if (ConvertibleValues.class == type || Object.class == type) {
            if (cachedBody instanceof ConvertibleValues) {
                return Optional.of((T) cachedBody);
            }
            final Map map = codec.decode(Map.class, byteBody.toInputStream());
            ConvertibleValues result = ConvertibleValues.of(map);
            cachedBody = result;
            return Optional.of((T) result);
        } else {
            if (cachedBody != null && cachedBody.getClass().isAssignableFrom(type)) {
                return Optional.of((T) cachedBody);
            }
            final T value = consumeBody(inputStream -> codec.decode(arg, inputStream));
            cachedBody = value;
            return Optional.of(value);
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
                    SimpleCookies simpleCookies = new SimpleCookies(conversionService);
                    simpleCookies.putAll(parseCookiesFromHeader());
                    this.cookies = simpleCookies;
                    cookies = simpleCookies;
                }
            }
        }
        return cookies;
    }

    private Map<CharSequence, Cookie> parseCookiesFromHeader() {
        Set<Cookie> result = new HashSet<>();
        for (String header: gatewayContext.getHeaders().getAllValues(COOKIE_HEADER)) {
            for (io.netty.handler.codec.http.cookie.Cookie cookie : ServerCookieDecoder.LAX.decode(header)) {
                result.add(new NettyCookie(cookie));
            }
        }
        return result.stream().collect(Collectors.toMap(Cookie::getName, Function.identity()));
    }

    @NonNull
    @Override
    public MutableHttpParameters getParameters() {
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
        if (uri == null) {
            synchronized (this) {
                if (uri == null) {
                    uri = URI.create(gatewayContext.getRequestURL());
                }
            }
        }
        return uri;
    }

    @Override
    public MutableHttpRequest<B> cookie(Cookie cookie) {
        // no-op, as cookies are not supported
        return this;
    }

    @Override
    public MutableHttpRequest<B> uri(URI uri) {
        synchronized (this) {
            this.uri = uri;
        }
        return this;
    }

    @Override
    public <T> MutableHttpRequest<T> body(T body) {
        // no-op, as body cannot be changed
        return (FnServletRequest<T>) body;
    }

    @NonNull
    @Override
    public MutableHttpHeaders getHeaders() {
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

    @Override
    public void setConversionService(@NonNull ConversionService conversionService) {
        // No-op
    }

    public boolean isFormSubmission() {
        MediaType contentType = getContentType().orElse(null);
        return MediaType.APPLICATION_FORM_URLENCODED_TYPE.equals(contentType)
            || MediaType.MULTIPART_FORM_DATA_TYPE.equals(contentType);
    }

    private ConvertibleMultiValues<CharSequence> parseFormData(String body) {
        Map parameterValues = new QueryStringDecoder(body, false).parameters();

        // Remove empty values
        Iterator<Entry<String, List<CharSequence>>> iterator = parameterValues.entrySet().iterator();
        while (iterator.hasNext()) {
            List<CharSequence> value = iterator.next().getValue();
            if (value.isEmpty() || StringUtils.isEmpty(value.get(0))) {
                iterator.remove();
            }
        }

        return new ConvertibleMultiValuesMap<CharSequence>(parameterValues, conversionService);
    }

    @Override
    public MutableHttpRequest<B> mutate() {
        FnServletRequest<B> request = new FnServletRequest<>(
            byteBody,
            inputEvent,
            response,
            gatewayContext,
            conversionService,
            codecRegistry
        );
        request.cookies = cookies;
        request.attributes = attributes;
        return request;
    }

    @Override
    public @NonNull ByteBody byteBody() {
        return byteBody;
    }

    /**
     * The fn parameters.
     */
    private final class FnHttpParameters implements MutableHttpParameters {

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

        @Override
        public MutableHttpParameters add(CharSequence name, List<CharSequence> values) {
            gatewayContext.getQueryParameters().getAll().put(name.toString(), values.stream().map(Object::toString).toList());
            return this;
        }

        @Override
        public void setConversionService(@NonNull ConversionService conversionService) {
            // no-op
        }
    }

    /**
     * The fn headers.
     */
    private final class FnHttpHeaders implements MutableHttpHeaders {

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

        @Override
        public MutableHttpHeaders add(CharSequence header, CharSequence value) {
            gatewayContext.getHeaders().addHeader(header.toString(), value.toString());
            return this;
        }

        @Override
        public MutableHttpHeaders remove(CharSequence header) {
            gatewayContext.getHeaders().removeHeader(header.toString());
            return this;
        }

        @Override
        public void setConversionService(@NonNull ConversionService conversionService) {
            // no-op
        }
    }

}
