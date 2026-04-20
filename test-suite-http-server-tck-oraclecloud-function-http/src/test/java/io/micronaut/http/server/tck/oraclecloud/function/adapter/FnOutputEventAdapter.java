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

import com.fnproject.fn.api.OutputEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.http.CaseInsensitiveMutableHttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapts the v2 {@link OutputEvent} to a {@link HttpResponse}.
 *
 * @param <B> The body type
 */
@Internal
public class FnOutputEventAdapter<B> implements HttpResponse<B> {
    private static final String SYNTHETIC_EMPTY_BODY_HEADER = "X-Micronaut-Fn-Synthetic-Empty-Body";
    private final OutputEvent event;
    private final ConversionService conversionService;
    private final FnHttpGatewayContextAdapter gatewayContext;
    private final MutableConvertibleValues<Object> attributes = new MutableConvertibleValuesMap<>();
    private byte[] cachedBytes;

    /**
     * Create the output event adapter.
     *
     * @param event The event
     * @param conversionService The conversion service
     */
    public FnOutputEventAdapter(
            OutputEvent event, FnHttpGatewayContextAdapter gatewayContext,
            ConversionService conversionService
    ) {
        this.event = event;
        this.gatewayContext = gatewayContext;
        this.conversionService = conversionService;
    }

    @Override
    public MutableHttpHeaders getHeaders() {
        Map<String, List<String>> eventHeaders = event.getHeaders().asMap();
        Map<String, List<String>> gatewayHeaders = gatewayContext.getResponseHeaders();
        Map<String, List<String>> mergedHeaders = mergeHeaders(eventHeaders, gatewayHeaders);
        if (isSyntheticEmptyBodyResponse()) {
            mergedHeaders.remove("Content-Type");
        }
        mergedHeaders.remove(SYNTHETIC_EMPTY_BODY_HEADER);
        return new CaseInsensitiveMutableHttpHeaders(
            mergedHeaders,
            conversionService
        );
    }

    @Override
    public MutableConvertibleValues<Object> getAttributes() {
        return attributes;
    }

    @Override
    public Optional<B> getBody() {
        byte[] bytes = responseBytes();
        if (isSyntheticEmptyBodyResponse()) {
            return Optional.empty();
        }
        if (bytes.length == 0) {
            return Optional.empty();
        }
        return (Optional<B>) Optional.of(bytes);
    }

    @Override
    public int code() {
        Integer status = gatewayContext.getStatusCode();
        return status == null ? event.getStatus().getCode() : status;
    }

    @Override
    public String reason() {
        return getStatus().getReason();
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.valueOf(code());
    }

    private Map<String, List<String>> mergeHeaders(
        Map<String, List<String>> eventHeaders,
        Map<String, List<String>> gatewayHeaders
    ) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        eventHeaders.forEach((key, value) -> {
            if (key.startsWith("Fn-Http-H-")) {
                key = key.substring("Fn-Http-H-".length());
            }
            if (!key.startsWith("Fn-Http-")) {
                result.put(key, new ArrayList<>(value));
            }
        });
        gatewayHeaders.forEach((key, value) -> {
            String normalizedKey = key.startsWith("Fn-Http-H-") ? key.substring("Fn-Http-H-".length()) : key;
            if (!normalizedKey.startsWith("Fn-Http-")) {
                List<String> existing = result.get(normalizedKey);
                boolean eventHeaderMissing = existing == null || existing.isEmpty();
                boolean gatewayHeaderPresent = value != null && !value.isEmpty();
                if (eventHeaderMissing || gatewayHeaderPresent) {
                    result.put(normalizedKey, new ArrayList<>(value));
                }
            }
        });
        return result;
    }

    private byte[] responseBytes() {
        if (cachedBytes == null) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                event.writeToOutput(bos);
                cachedBytes = bos.toByteArray();
            } catch (IOException e) {
                cachedBytes = new byte[0];
            }
        }
        return cachedBytes;
    }

    private boolean isSyntheticEmptyBodyResponse() {
        return event.getHeaders().get(SYNTHETIC_EMPTY_BODY_HEADER).isPresent()
            || event.getHeaders().get("Fn-Http-H-" + SYNTHETIC_EMPTY_BODY_HEADER).isPresent()
            || gatewayContext.getResponseHeaders().containsKey(SYNTHETIC_EMPTY_BODY_HEADER)
            || gatewayContext.getResponseHeaders().containsKey("Fn-Http-H-" + SYNTHETIC_EMPTY_BODY_HEADER);
    }

}
