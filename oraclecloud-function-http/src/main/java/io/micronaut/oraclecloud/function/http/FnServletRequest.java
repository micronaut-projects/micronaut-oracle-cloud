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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
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

    @SuppressWarnings("rawtypes")
    static final Argument<ConvertibleValues> CONVERTIBLE_VALUES_ARGUMENT = Argument.of(ConvertibleValues.class);

    private static final String COOKIE_HEADER = "Cookie";
    private static final String FN_HTTP_HEADER_PREFIX = "Fn-Http-H-";
    private static final String LOCALHOST = "localhost";

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
        for (String header: getHeaders().getAll(COOKIE_HEADER)) {
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
                    uri = buildUri(URI.create(gatewayContext.getRequestURL()));
                }
            }
        }
        return uri;
    }

    @Override
    public @NonNull String getPath() {
        String path = getUri().getRawPath();
        return StringUtils.isEmpty(path) ? "/" : path;
    }

    @Override
    public @NonNull String getContextPath() {
        return "";
    }

    public @NonNull String getPathInfo() {
        return getUri().getRawPath();
    }

    public @NonNull String getServletPath() {
        return getUri().getRawPath();
    }

    public @Nullable String getQueryString() {
        return getUri().getRawQuery();
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
        return (FnServletRequest<T>) this;
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

    @Override
    public @NonNull InetSocketAddress getRemoteAddress() {
        InetSocketAddress sourceIpAddress = resolveSourceIpAddress();
        if (sourceIpAddress != null) {
            return sourceIpAddress;
        }

        InetSocketAddress forwardedForAddress = resolveUnresolvedAddress(getHeaders().get("X-Forwarded-For"));
        if (forwardedForAddress != null) {
            return forwardedForAddress;
        }

        InetSocketAddress realIpAddress = resolveUnresolvedAddress(getHeaders().get("X-Real-Ip"));
        if (realIpAddress != null) {
            return realIpAddress;
        }

        InetSocketAddress forwardedAddress = resolveForwardedAddress(getHeaders().get("Forwarded"));
        if (forwardedAddress != null) {
            return forwardedAddress;
        }

        return new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
    }

    @Override
    public @NonNull InetSocketAddress getServerAddress() {
        URI currentUri = getUri();
        String host = currentUri.getHost();
        int port = currentUri.getPort();
        if (StringUtils.isEmpty(host)) {
            host = LOCALHOST;
        }
        if (port < 0) {
            port = currentUri.getScheme() != null && currentUri.getScheme().equalsIgnoreCase("https") ? 443 : 80;
        }
        return InetSocketAddress.createUnresolved(host, port);
    }

    @Override
    public @NonNull String getServerName() {
        String host = getUri().getHost();
        if (StringUtils.isEmpty(host)) {
            host = LOCALHOST;
        }
        return host;
    }

    private URI buildUri(URI requestUri) {
        String path = normalizePath(requestUri.getRawPath());
        String query = requestUri.getRawQuery();
        if (hasAuthority(requestUri)) {
            return URI.create(buildAbsoluteUri(requestUri, path, query));
        }
        return URI.create(appendQuery(path, query));
    }

    private String normalizePath(String path) {
        return StringUtils.isEmpty(path) ? "/" : path;
    }

    private boolean hasAuthority(URI requestUri) {
        return StringUtils.isNotEmpty(requestUri.getRawAuthority())
            || StringUtils.isNotEmpty(requestUri.getScheme())
            || StringUtils.isNotEmpty(requestUri.getHost());
    }

    private String buildAbsoluteUri(URI requestUri, String path, String query) {
        String scheme = StringUtils.isEmpty(requestUri.getScheme()) ? "http" : requestUri.getScheme();
        return scheme + "://" + resolveAuthority(requestUri) + appendQuery(path, query);
    }

    private String resolveAuthority(URI requestUri) {
        String authority = requestUri.getRawAuthority();
        if (StringUtils.isNotEmpty(authority)) {
            return authority;
        }
        String host = StringUtils.isEmpty(requestUri.getHost()) ? LOCALHOST : requestUri.getHost();
        int port = requestUri.getPort();
        return port >= 0 ? host + ":" + port : host;
    }

    private String appendQuery(String path, String query) {
        return query == null ? path : path + "?" + query;
    }

    private InetSocketAddress resolveSourceIpAddress() {
        String sourceIp = inputEvent.getHeaders().get("Fn-Http-Source-Ip").orElse(null);
        if (StringUtils.isEmpty(sourceIp)) {
            sourceIp = inputEvent.getHeaders().get(FN_HTTP_HEADER_PREFIX + "Fn-Http-Source-Ip").orElse(null);
        }
        return resolveSourceIpAddress(sourceIp);
    }

    private InetSocketAddress resolveSourceIpAddress(String sourceIp) {
        String host = extractFirstHost(sourceIp);
        if (StringUtils.isEmpty(host)) {
            return null;
        }
        try {
            return new InetSocketAddress(InetAddress.getByName(host), 0);
        } catch (UnknownHostException ignored) {
            return InetSocketAddress.createUnresolved(host, 0);
        }
    }

    private InetSocketAddress resolveUnresolvedAddress(String headerValue) {
        String host = extractFirstHost(headerValue);
        return StringUtils.isEmpty(host) ? null : InetSocketAddress.createUnresolved(host, 0);
    }

    private String extractFirstHost(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String host = value.split(",", 2)[0].trim();
        return StringUtils.isEmpty(host) ? null : host;
    }

    private InetSocketAddress resolveForwardedAddress(String forwarded) {
        if (StringUtils.isEmpty(forwarded)) {
            return null;
        }
        for (String part : forwarded.split("[;,]")) {
            String host = extractForwardedHost(part);
            if (StringUtils.isNotEmpty(host)) {
                try {
                    return new InetSocketAddress(InetAddress.getByName(host), 0);
                } catch (UnknownHostException ignored) {
                    return new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
                }
            }
        }
        return null;
    }

    private String extractForwardedHost(String part) {
        String trimmed = part.trim();
        if (!trimmed.regionMatches(true, 0, "for=", 0, 4)) {
            return null;
        }
        return normalizeForwardedHost(trimmed.substring(4).trim());
    }

    private String normalizeForwardedHost(String host) {
        if (host.startsWith("\"") && host.endsWith("\"") && host.length() > 1) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.startsWith("[")) {
            int closing = host.indexOf(']');
            return closing > 0 ? host.substring(1, closing) : host;
        }
        int colon = host.indexOf(':');
        return colon > 0 ? host.substring(0, colon) : host;
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

        private String normalizeHeaderName(String name) {
            return name.startsWith(FN_HTTP_HEADER_PREFIX) ? name.substring(FN_HTTP_HEADER_PREFIX.length()) : name;
        }

        private List<String> getNormalizedValues(String name) {
            List<String> direct = inputEvent.getHeaders().getAllValues(name);
            if (!direct.isEmpty()) {
                return direct;
            }
            return inputEvent.getHeaders().getAllValues(FN_HTTP_HEADER_PREFIX + name);
        }

        @Override
        public List<String> getAll(CharSequence name) {
            if (name != null) {
                return getNormalizedValues(name.toString());
            }
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public String get(CharSequence name) {
            if (name != null) {
                List<String> values = getNormalizedValues(name.toString());
                if (!values.isEmpty()) {
                    return values.get(0);
                }
            }
            return null;
        }

        @Override
        public Set<String> names() {
            return inputEvent.getHeaders().keys().stream()
                .map(this::normalizeHeaderName)
                .collect(Collectors.toCollection(HashSet::new));
        }

        @Override
        public Collection<List<String>> values() {
            return names().stream()
                .map(this::getNormalizedValues)
                .toList();
        }

        @Override
        public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
            if (name != null) {
                Optional<String> v = Optional.ofNullable(get(name));
                return v.flatMap(s -> conversionService.convert(
                    s, conversionContext
                ));
            }
            return Optional.empty();
        }

        @Override
        public MutableHttpHeaders add(CharSequence header, CharSequence value) {
            response.getHeaders().add(header, value);
            return this;
        }

        @Override
        public MutableHttpHeaders remove(CharSequence header) {
            response.getHeaders().remove(header);
            return this;
        }

        @Override
        public void setConversionService(@NonNull ConversionService conversionService) {
            // no-op
        }
    }

}
