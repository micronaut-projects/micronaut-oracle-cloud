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
package io.micronaut.oraclecloud.function.http.test;

import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.testing.FnEventBuilder;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.oraclecloud.function.http.FnMultiValueMap;
import io.micronaut.oraclecloud.function.http.HttpFunction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.OK;

/**
 * Testing support for functions.
 */
public final class FnHttpTest {

    private static final Argument<String> STRING = Argument.of(String.class);

    /**
     * Invoke a function via HTTP.
     * @param request The request
     * @param <I> The body type
     * @return The result
     */
    public static <I> HttpResponse<String> invoke(HttpRequest<I> request) {
        return invoke(request, STRING, null);
    }

    /**
     * Invoke a function via HTTP.
     * @param request The request
     * @param sharedClasses optional classes to share between the test classloader and the FN classloader
     * @param <I> The body type
     * @return The result
     */
    public static <I> HttpResponse<String> invoke(HttpRequest<I> request,
                                                  List<Class<?>> sharedClasses) {
        return invoke(request, STRING, sharedClasses);
    }

    /**
     * Invoke a function via HTTP.
     * @param method The http method
     * @param uri The uri
     * @param <I> The body type
     * @return The result
     */
    public static <I> HttpResponse<String> invoke(HttpMethod method, String uri) {
        return invoke(HttpRequest.create(method, uri), STRING, null);
    }

    /**
     * Invoke a function via HTTP.
     * @param method The http method
     * @param uri The uri
     * @param sharedClasses optional classes to share between the test classloader and the FN classloader
     * @param <I> The body type
     * @return The result
     */
    public static <I> HttpResponse<String> invoke(HttpMethod method, String uri,
                                                  List<Class<?>> sharedClasses) {
        return invoke(HttpRequest.create(method, uri), STRING, sharedClasses);
    }

    /**
     * Invoke a function via HTTP.
     * @param request The request
     * @param resultType The result type
     * @param <I> The input type
     * @param <O> The output type
     * @return The response
     */
    public static <I, O> HttpResponse<O> invoke(HttpRequest<I> request,
                                                Class<O> resultType) {
        return invoke(request, Argument.of(resultType), null);
    }

    /**
     * Invoke a function via HTTP.
     * @param request The request
     * @param resultType The result type
     * @param sharedClasses optional classes to share between the test classloader and the FN classloader
     * @param <I> The input type
     * @param <O> The output type
     * @return The response
     */
    public static <I, O> HttpResponse<O> invoke(HttpRequest<I> request,
                                                Class<O> resultType,
                                                List<Class<?>> sharedClasses) {
        return invoke(request, Argument.of(resultType), sharedClasses);
    }

    /**
     * Invoke a function via HTTP.
     * @param request The request
     * @param resultType The result type
     * @param <I> The input type
     * @param <O> The output type
     * @return The response
     */
    public static <I, O> HttpResponse<O> invoke(HttpRequest<I> request,
                                                Argument<O> resultType) {
        return invoke(request, resultType, null);
    }

    /**
     * Invoke a function via HTTP.
     * @param request The request
     * @param resultType The result type
     * @param sharedClasses optional classes to share between the test classloader and the FN classloader
     * @param <I> The input type
     * @param <O> The output type
     * @return The response
     */
    public static <I, O> HttpResponse<O> invoke(HttpRequest<I> request,
                                                Argument<O> resultType,
                                                List<Class<?>> sharedClasses) {
        try (ApplicationContext ctx = ApplicationContext.run()) {
            final MediaTypeCodecRegistry codecRegistry = ctx.getBean(MediaTypeCodecRegistry.class);
            final ConversionService conversionService = ctx.getBean(ConversionService.class);
            Objects.requireNonNull(request, "The request cannot be null");
            Objects.requireNonNull(resultType, "The result type cannot be null");

            FnTestingRule fn = FnTestingRule.createDefault();
            fn.addSharedClassPrefix("org.slf4j.");
            fn.addSharedClassPrefix("com.sun.");
            if (sharedClasses != null) {
                for (Class<?> c : sharedClasses) {
                    fn.addSharedClass(c);
                }
            }

            FnEventBuilder<FnTestingRule> eventBuilder = fn.givenEvent()
                    .withHeader("Fn-Http-Request-Url", request.getUri().toString())
                    .withHeader("Fn-Http-Method", request.getMethodName());

            request.getHeaders().forEach((s, values) -> {
                for (String v : values) {
                    eventBuilder.withHeader("Fn-Http-H-" + s, v);
                }
            });
            I b = request.getBody().orElse(null);
            if (b instanceof byte[]) {
                eventBuilder.withBody((byte[]) b);
            } else if (b instanceof CharSequence)  {
                eventBuilder.withBody(
                        b.toString().getBytes(request.getCharacterEncoding())
                );
            } else if (b != null) {
                final MediaTypeCodec codec = request.getContentType().flatMap(codecRegistry::findCodec).orElse(null);
                if (codec != null) {
                    eventBuilder.withBody(codec.encode(b));
                } else {
                    eventBuilder.withBody(conversionService.convertRequired(b, byte[].class));
                }
            }

            eventBuilder.enqueue();
            fn.thenRun(HttpFunction.class, "handleRequest");
            FnResult fnResult = fn.getOnlyResult();

            return new FnHttpResponse<>(fnResult, resultType, codecRegistry, conversionService);
        }
    }

    /**
     * A response wrapper.
     * @param <B> The body type
     */
    private static final class FnHttpResponse<B> implements HttpResponse<B> {
        private final FnResult outputEvent;
        private final FnHeaders fnHeaders;
        private final Argument<B> resultType;
        private final MediaTypeCodecRegistry codecRegistry;

        private final ConversionService conversionService;
        private MutableConvertibleValues<Object> attributes;

        public FnHttpResponse(FnResult outputEvent, Argument<B> resultType,
                              MediaTypeCodecRegistry codecRegistry,
                              ConversionService conversionService) {
            this.outputEvent = outputEvent;
            this.resultType = resultType;
            this.codecRegistry = codecRegistry;
            this.conversionService = conversionService;
            Map<String, List<String>> headers = new LinkedHashMap<>();
            outputEvent.getHeaders().asMap().forEach((key, strings) -> {
                if (key.startsWith("Fn-Http-H-")) {
                    String httpKey = key.substring("Fn-Http-H-".length());
                    if (httpKey.length() > 0) {
                        headers.put(httpKey, strings);
                    }
                }
            });
            this.fnHeaders = new FnHeaders(headers, conversionService);
        }

        @Override
        public HttpStatus getStatus() {
            return outputEvent.getHeaders().get("Fn-Http-Status").map(s ->
                    HttpStatus.valueOf(Integer.parseInt(s))).orElseGet(() ->
                outputEvent.getStatus() == OutputEvent.Status.Success ? OK : INTERNAL_SERVER_ERROR
            );
        }

        @Override
        public int code() {
            return getStatus().getCode();
        }

        @Override
        public String reason() {
            return getStatus().getReason();
        }

        @NonNull
        @Override
        public <T> Optional<T> getBody(@NonNull Argument<T> type) {
            return conversionService.convert(outputEvent.getBodyAsBytes(), type);
        }

        @NonNull
        @Override
        public HttpHeaders getHeaders() {
            return fnHeaders;
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
            if (CharSequence.class.isAssignableFrom(resultType.getType())) {
                return (Optional<B>) Optional.of(outputEvent.getBodyAsString());
            } else {
                final MediaTypeCodec codec = getContentType().flatMap(codecRegistry::findCodec).orElse(null);
                final byte[] bodyAsBytes = outputEvent.getBodyAsBytes();
                if (codec != null) {
                    final B result = codec.decode(resultType, bodyAsBytes);
                    return Optional.ofNullable(result);
                } else {
                    return conversionService.convert(bodyAsBytes, resultType);
                }
            }
        }

        @NonNull
        @Override
        public Optional<MediaType> getContentType() {
            return outputEvent.getContentType().map(MediaType::new);
        }

        /**
         * The headers.
         */
        private static final class FnHeaders
                extends FnMultiValueMap implements HttpHeaders {

            /**
             * Default constructor.
             *
             * @param map The target map. Never null
             */
            public FnHeaders(Map<String, List<String>> map, ConversionService conversionService) {
                super(map, conversionService);
            }
        }
    }
}
