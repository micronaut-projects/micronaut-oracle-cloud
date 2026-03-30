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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.bind.binders.AnnotatedRequestArgumentBinder;
import io.micronaut.http.bind.binders.DefaultBodyAnnotationBinder;
import io.micronaut.http.body.MessageBodyHandlerRegistry;
import io.micronaut.http.body.MessageBodyReader;
import io.micronaut.http.codec.CodecException;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.json.body.JsonMessageHandler;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Adds the ability to bind the request body.
 * @param <T> The body type.
 * @since 1.0.0
 */
@Internal
final class FnBodyBinder<T> implements AnnotatedRequestArgumentBinder<Body, T> {
    private static final Logger LOG = LoggerFactory.getLogger(FnBodyBinder.class);
    private final MessageBodyHandlerRegistry messageBodyHandlerRegistry;
    private final DefaultBodyAnnotationBinder<T> defaultBodyBinder;
    private final ConversionService conversionService;

    /**
     * Default constructor.
     *
     * @param conversionService      The conversion service
     * @param mediaTypeCodecRegistry The codec registry
     */
    protected FnBodyBinder(
            ConversionService conversionService,
            MessageBodyHandlerRegistry messageBodyHandlerRegistry,
            DefaultBodyAnnotationBinder<T> defaultBodyAnnotationBinder) {
        this.defaultBodyBinder = defaultBodyAnnotationBinder;
        this.messageBodyHandlerRegistry = messageBodyHandlerRegistry;
        this.conversionService = conversionService;
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, HttpRequest<?> source) {
        final Argument<T> argument = context.getArgument();
        final Class<T> type = argument.getType();
        String name = argument.getAnnotationMetadata().stringValue(Body.class).orElse(null);
        if (source instanceof FnServletRequest) {
            FnServletRequest<?> servletHttpRequest = (FnServletRequest<?>) source;
            if (CharSequence.class.isAssignableFrom(type) && name == null) {
                return servletHttpRequest.consumeBody(inputStream -> {
                    try {
                        String content = IOUtils.readText(new BufferedReader(new InputStreamReader(
                            inputStream, source.getCharacterEncoding()
                        )));
                        LOG.trace("Read content of length {} from function body", content.length());
                        return () -> (Optional<T>) Optional.of(content);
                    } catch (IOException e) {
                        LOG.debug("Error occurred reading function body: {}", e.getMessage(), e);
                        return new ConversionFailedBindingResult<>(e);
                    }
                });
            } else {
                final MediaType mediaType = source.getContentType().orElse(MediaType.APPLICATION_JSON_TYPE);
                if (servletHttpRequest.isFormSubmission()) {
                    return bindFormData(servletHttpRequest, name, context);
                }

                BindingResult<T> bindingResult = servletHttpRequest.consumeBody(inputStream -> {
                    try {
                        if (Publishers.isConvertibleToPublisher(type)) {
                            return bindPublisher(argument, type, mediaType, inputStream, source);
                        } else {
                            return bindPojo(argument, type, mediaType, inputStream, name, source);
                        }
                    } catch (CodecException e) {
                        LOG.trace("Error occurred decoding function body: {}", e.getMessage(), e);
                        return new ConversionFailedBindingResult<>(e);
                    }
                });
                if (bindingResult instanceof ConversionFailedBindingResult<?>) {
                    return bindingResult;
                }
                Optional<T> decoded = bindingResult.getValue();
                if (decoded.isEmpty()) {
                    LOG.trace("No message body reader matched type {}, falling back to default body decoding", argument);
                    return defaultBodyBinder.bind(context, source);
                }
                return bindingResult;
            }
        }
        LOG.trace("Not a function request, falling back to default body decoding");
        return defaultBodyBinder.bind(context, source);
    }

    private BindingResult<T> bindFormData(
        FnServletRequest<?> servletHttpRequest, String name, ArgumentConversionContext<T> context
    ) {
        Optional<ConvertibleValues> form = servletHttpRequest.getBody(FnServletRequest.CONVERTIBLE_VALUES_ARGUMENT);
        if (form.isEmpty()) {
            return BindingResult.empty();
        }
        if (name != null) {
            return () -> form.get().get(name, context);
        }
        return () -> conversionService.convert(form.get().asMap(), context);
    }

    @SuppressWarnings("unchecked")
    private MessageBodyReader<Object> findReader(Argument<?> type, MediaType mediaType) {
        return (MessageBodyReader<Object>) messageBodyHandlerRegistry
            .findReader((Argument<Object>) (Argument<?>) type, mediaType)
            .orElse(null);
    }

    private BindingResult<T> bindPojo(
        Argument<T> argument,
        Class<?> type,
        MediaType mediaType,
        InputStream inputStream,
        @Nullable String name,
        HttpRequest<?> source
    ) {
        Argument<?> requiredArg = type.isArray() ? Argument.listOf(type.getComponentType()) : argument;
        MessageBodyReader<Object> reader = findReader(requiredArg, mediaType);
        if (reader == null) {
            return BindingResult.empty();
        }

        Object converted;
        if (name != null && reader instanceof JsonMessageHandler<?> jsonHandler) {
            try {
                JsonNode node = jsonHandler.getJsonMapper().readValue(inputStream, JsonNode.class);
                JsonNode field = node.get(name);
                if (field == null) {
                    return BindingResult.empty();
                }
                converted = jsonHandler.getJsonMapper().readValueFromTree(field, (Argument<Object>) requiredArg);
            } catch (IOException e) {
                throw new CodecException("Error decoding JSON stream for type [JsonNode]: " + e.getMessage(), e);
            }
        } else {
            converted = reader.read((Argument<Object>) requiredArg, mediaType, source.getHeaders(), inputStream);
        }

        if (converted == null) {
            return BindingResult.empty();
        }

        if (type.isArray()) {
            converted = ((List<?>) converted).toArray((Object[]) Array.newInstance(type.getComponentType(), 0));
        }
        T content = (T) converted;
        LOG.trace("Decoded object from function body: {}", converted);
        return () -> Optional.of(content);
    }

    private BindingResult<T> bindPublisher(
        Argument<T> argument,
        Class<T> type,
        MediaType mediaType,
        InputStream inputStream,
        HttpRequest<?> source
    ) {
        final Argument<?> typeArg = argument.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT);
        if (Publishers.isSingle(type)) {
            MessageBodyReader<Object> reader = findReader(typeArg, mediaType);
            if (reader == null) {
                return BindingResult.empty();
            }
            Object decoded = reader.read((Argument<Object>) typeArg, mediaType, source.getHeaders(), inputStream);
            if (decoded == null) {
                return BindingResult.empty();
            }
            final Publisher<?> publisher = Publishers.just(decoded);
            LOG.trace("Decoded object from function body: {}", decoded);
            final T converted = conversionService.convertRequired(publisher, type);
            return () -> Optional.of(converted);
        } else {
            final Argument<List<?>> containerType = (Argument<List<?>>) Argument.listOf(typeArg.getType());
            MessageBodyReader<Object> reader = findReader(containerType, mediaType);
            if (reader instanceof JsonMessageHandler<?> jsonHandler) {
                try {
                    JsonNode node = jsonHandler.getJsonMapper().readValue(inputStream, JsonNode.class);
                    Iterable<?> iterable;
                    if (node.isArray()) {
                        iterable = (Iterable<?>) jsonHandler.getJsonMapper().readValueFromTree(node, (Argument) containerType);
                    } else {
                        Object single = jsonHandler.getJsonMapper().readValueFromTree(node, (Argument<Object>) typeArg);
                        iterable = List.of(single);
                    }
                    LOG.trace("Decoded object from function body: {}", iterable);
                    Flux<?> flux = Flux.fromIterable(iterable);
                    final T converted = Publishers.convertPublisher(conversionService, flux, type);
                    return () -> Optional.of(converted);
                } catch (IOException e) {
                    throw new CodecException("Error decoding JSON stream for type [JsonNode]: " + e.getMessage(), e);
                }
            }
            if (reader != null) {
                Object decoded = reader.read((Argument<Object>) containerType, mediaType, source.getHeaders(), inputStream);
                if (decoded == null) {
                    return BindingResult.empty();
                }
                LOG.trace("Decoded object from function body: {}", decoded);
                Flux<?> flux = Flux.fromIterable((Iterable<?>) decoded);
                final T converted = Publishers.convertPublisher(conversionService, flux, type);
                return () -> Optional.of(converted);
            }
            MessageBodyReader<Object> elementReader = findReader(typeArg, mediaType);
            if (elementReader == null) {
                return BindingResult.empty();
            }
            Object element = elementReader.read((Argument<Object>) typeArg, mediaType, source.getHeaders(), inputStream);
            if (element == null) {
                return BindingResult.empty();
            }
            LOG.trace("Decoded object from function body: {}", element);
            Flux<?> flux = Flux.just(element);
            final T converted = Publishers.convertPublisher(conversionService, flux, type);
            return () -> Optional.of(converted);
        }
    }

    @Override
    public Class<Body> getAnnotationType() {
        return Body.class;
    }

    /**
     * A binding result implementation for the case when conversion error was thrown.
     *
     * @param <T> The type to be bound
     * @param e The conversion error
     */
    private record ConversionFailedBindingResult<T>(
        Exception e
    ) implements BindingResult<T> {

        @Override
        public Optional<T> getValue() {
            return Optional.empty();
        }

        @Override
        public List<ConversionError> getConversionErrors() {
            return Collections.singletonList(() -> e);
        }

    }

}
