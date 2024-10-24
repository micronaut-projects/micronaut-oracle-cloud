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
import java.util.HashMap;
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

    private final OutputEvent event;
    private final ConversionService conversionService;
    private final FnHttpGatewayContextAdapter gatewayContext;
    private final MutableConvertibleValues<Object> attributes = new MutableConvertibleValuesMap<>();

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
        return new CaseInsensitiveMutableHttpHeaders(
            mergeHeaders(gatewayContext.getResponseHeaders(), event.getHeaders().asMap()),
            conversionService
        );
    }

    @Override
    public MutableConvertibleValues<Object> getAttributes() {
        return attributes;
    }

    @Override
    public Optional<B> getBody() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            event.writeToOutput(bos);
            byte[] bytes = bos.toByteArray();
            if (bytes.length == 0) {
                return Optional.empty();
            }
            return (Optional<B>) Optional.of(bytes);
        } catch (IOException e) {
            return Optional.empty();
        }
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
        Map<String, List<String>> first,
        Map<String, List<String>> other
    ) {
        Map<String, List<String>> result = new HashMap<>();
        first.forEach((key, value) -> {
            // TODO understand why the headers start this way
            if (key.startsWith("Fn-Http-H-")) {
                key = key.substring("Fn-Http-H-".length());
            }
            result.put(key, value);
        });
        other.forEach((key, value) -> {
            if (key.startsWith("Fn-Http-H-")) {
                key = key.substring("Fn-Http-H-".length());
            }
            if (result.containsKey(key)) {
                result.get(key).addAll(value);
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

}
