/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.http.server.tck.oraclecloud.function.adapter;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.body.MessageBodyHandlerRegistry;
import io.micronaut.http.body.MessageBodyWriter;
import io.micronaut.http.simple.SimpleHttpHeaders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A utility factory for creating {@link InputEvent}s.
 */
@Internal
public final class FnInputEventFactory {

    /**
     * Create the input event.
     *
     * @param request The request
     * @return The input event
     */
    public static InputEvent create(@NonNull HttpRequest<?> request, MessageBodyHandlerRegistry messageHandlerRegistry) {
        String callId = UUID.randomUUID().toString();
        return new ReadOnceInputEvent(
            createBody(request, messageHandlerRegistry),
            createHeaders(request),
            callId,
            Instant.now().plus(Duration.of(1, ChronoUnit.MINUTES))
        );
    }

    private static <T> InputStream createBody(@NonNull HttpRequest<T> request, MessageBodyHandlerRegistry messageHandlerRegistry) {
        Optional<byte[]> bodyBytes = request.getBody(byte[].class);
        if (bodyBytes.isPresent()) {
            return new ByteArrayInputStream(bodyBytes.get());
        }

        Optional<T> optionalBody = request.getBody();
        if (optionalBody.isPresent()) {
            MediaType mediaType = request.getContentType().orElse(MediaType.APPLICATION_JSON_TYPE);
            Argument<T> argument = (Argument) Argument.of(optionalBody.get().getClass());
            Optional<MessageBodyWriter<T>> writer = messageHandlerRegistry.findWriter(argument, List.of(mediaType));
            if (writer.isPresent() && writer.get().isWriteable(argument, mediaType)) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                // Basically ignore the headers. This can be changed if there are tests that
                // require setting specific headers.
                writer.get().writeTo(
                    argument, mediaType, optionalBody.get(),
                    new SimpleHttpHeaders(ConversionService.SHARED), outputStream
                );
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
        }
        return new ByteArrayInputStream(new byte[0]);
    }

    private static Headers createHeaders(@NonNull HttpRequest<?> request) {
        Map<String, List<String>> headers = request.getHeaders().asMap()
            .entrySet().stream()
            .collect(Collectors.toMap(
                    e -> Headers.canonicalKey(e.getKey()),
                    Entry::getValue
            ));
        return Headers.fromMultiHeaderMap(headers);
    }

}
