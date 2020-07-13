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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.bind.binders.AnnotatedRequestArgumentBinder;
import io.micronaut.http.bind.binders.DefaultBodyAnnotationBinder;
import io.micronaut.http.codec.MediaTypeCodec;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import java.io.BufferedReader;
import java.io.IOException;
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
final class FnBodyBinder<T> extends DefaultBodyAnnotationBinder<T> implements AnnotatedRequestArgumentBinder<Body, T> {
    private final MediaTypeCodecRegistry mediaTypeCodeRegistry;

    /**
     * Default constructor.
     *
     * @param conversionService      The conversion service
     * @param mediaTypeCodecRegistry The codec registry
     */
    protected FnBodyBinder(
            ConversionService<?> conversionService,
            MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        super(conversionService);
        this.mediaTypeCodeRegistry = mediaTypeCodecRegistry;
    }

    @Override
    public Class<Body> getAnnotationType() {
        return Body.class;
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, HttpRequest<?> source) {
        final Argument<T> argument = context.getArgument();
        final Class<T> type = argument.getType();
        String name = argument.getAnnotationMetadata().stringValue(Body.class).orElse(null);
        if (source instanceof FnServletRequest) {
            FnServletRequest<?> servletHttpRequest = (FnServletRequest<?>) source;
            if (CharSequence.class.isAssignableFrom(type) && name == null) {
                return servletHttpRequest.getNativeRequest().consumeBody(inputStream -> {
                    try {
                        final String content = IOUtils.readText(new BufferedReader(new InputStreamReader(inputStream, source.getCharacterEncoding())));
                        return () -> (Optional<T>) Optional.of(content);
                    } catch (IOException e) {
                        return new BindingResult<T>() {
                            @Override
                            public Optional<T> getValue() {
                                return Optional.empty();
                            }

                            @Override
                            public List<ConversionError> getConversionErrors() {
                                return Collections.singletonList(
                                        () -> e
                                );
                            }
                        };
                    }
                });

            } else {
                final MediaType mediaType = source.getContentType().orElse(MediaType.APPLICATION_JSON_TYPE);
                final MediaTypeCodec codec = mediaTypeCodeRegistry
                        .findCodec(mediaType, type)
                        .orElse(null);

                if (codec != null) {
                    return servletHttpRequest.getNativeRequest().consumeBody(inputStream -> {
                        if (Publishers.isConvertibleToPublisher(type)) {
                            final Argument<?> typeArg = argument.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT);
                            if (Publishers.isSingle(type)) {
                                T content = (T) codec.decode(typeArg, inputStream);
                                final Publisher<T> publisher = Publishers.just(content);
                                final T converted = conversionService.convertRequired(publisher, type);
                                return () -> Optional.of(converted);
                            } else {
                                final Argument<? extends List<?>> containerType = Argument.listOf(typeArg.getType());
                                T content = (T) codec.decode(containerType, inputStream);
                                final Flowable flowable = Flowable.fromIterable((Iterable) content);
                                final T converted = conversionService.convertRequired(flowable, type);
                                return () -> Optional.of(converted);
                            }
                        } else {
                            if (type.isArray()) {
                                Class<?> componentType = type.getComponentType();
                                List<T> content = (List<T>) codec.decode(Argument.listOf(componentType), inputStream);
                                Object[] array = content.toArray((Object[]) Array.newInstance(componentType, 0));
                                return () -> Optional.of((T) array);
                            } else {
                                T content = codec.decode(argument, inputStream);
                                return () -> Optional.of(content);
                            }
                        }
                    });

                }

            }
        }
        return super.bind(context, source);
    }
}
