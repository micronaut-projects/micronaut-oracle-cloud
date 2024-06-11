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
package io.micronaut.http.server.tck.oraclecloud.function;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.QueryParameters;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import com.fnproject.fn.runtime.httpgateway.QueryParametersImpl;
import io.micronaut.http.HttpRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An adapter for {@link HTTPGatewayContext}.
 */
public class FnHttpGatewayContextAdapter implements HTTPGatewayContext {

    private final HttpRequest<?> request;
    private final InputEvent inputEvent;
    private Integer statusCode;
    private final Map<String, List<String>> responseHeaders = new HashMap<>();

    /**
     * Create the adapter context.
     *
     * @param request The Micronaut HTTP request
     * @param inputEvent The input event
     */
    public FnHttpGatewayContextAdapter(HttpRequest<?> request, InputEvent inputEvent) {
        this.request = request;
        this.inputEvent = inputEvent;
    }

    @Override
    public InvocationContext getInvocationContext() {
        return null;
    }

    @Override
    public Headers getHeaders() {
        return inputEvent.getHeaders();
    }

    @Override
    public String getRequestURL() {
        return request.getUri().toString();
    }

    @Override
    public String getMethod() {
        return request.getMethodName();
    }

    @Override
    public QueryParameters getQueryParameters() {
        return new QueryParametersImpl(request.getParameters().asMap());
    }

    @Override
    public void addResponseHeader(String key, String value) {
        if (!responseHeaders.containsKey(key)) {
            responseHeaders.put(key, new ArrayList<>());
        }
        responseHeaders.get(key).add(value);
    }

    @Override
    public void setResponseHeader(String key, String firstValue, String... otherValues) {
        List<String> values = new ArrayList<>(otherValues.length + 1);
        values.add(firstValue);
        values.addAll(Arrays.asList(otherValues));
        responseHeaders.put(key, values);
    }

    @Override
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Get the stored status code.
     * @return The status code
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Get the stored response headers.
     * @return The response headers
     */
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

}
