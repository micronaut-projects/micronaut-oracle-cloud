/*
 * Copyright 2017-2025 original authors
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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpStatus;
import io.micronaut.oraclecloud.function.http.HttpFunction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

@Experimental
@Internal
class FnHttpExchange implements HttpHandler {
    private final Set<String> environments;
    private Map<String, String> testConfig;

    FnHttpExchange(Map<String, String> testConfig,
                   Set<String> environments) {
        this.environments = environments;
        this.testConfig = testConfig;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        FnTestingRule fn = FnTestingRule.createDefault();
        testConfig.forEach(fn::setConfig);
        fn.addSharedClassPrefix("org.slf4j.");
        fn.addSharedClassPrefix("com.sun.");
        String requestURI = exchange.getRequestURI().toString();
        FnEventBuilder<FnTestingRule> eventBuilder = fn.givenEvent()
            .withHeader("Fn-Http-Request-Url", requestURI)
            .withHeader("Fn-Http-Method", exchange.getRequestMethod());
        Set<String> headerNames = exchange.getRequestHeaders().keySet();
        for (String headerName : headerNames) {
            eventBuilder.withHeader("Fn-Http-H-" + headerName, String.join(",", exchange.getRequestHeaders().get(headerName)));
        }
        HttpMethod httpMethod = HttpMethod.parse(exchange.getRequestMethod());
        if (HttpMethod.permitsRequestBody(httpMethod)) {
            try (InputStream requestBody = exchange.getRequestBody()) {
                eventBuilder.withBody(requestBody);
            } catch (IOException e) {
                // ignore
            }
        }

        eventBuilder.enqueue();

        // forward environments to function
        final String envs = System.getProperty("micronaut.environments");
        System.setProperty("micronaut.environments", String.join(",", environments));
        fn.thenRun(HttpFunction.class, "handleRequest");

        // re-set environment to previous value
        if (envs != null) {
            System.setProperty("micronaut.environments", envs);
        }
        FnResult outputEvent = fn.getOnlyResult();
        HttpStatus httpStatus = outputEvent.getHeaders().get("Fn-Http-Status").map(s ->
            HttpStatus.valueOf(Integer.parseInt(s))).orElseGet(() ->
            outputEvent.getStatus() == OutputEvent.Status.Success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR
        );
        byte[] bodyAsBytes = outputEvent.getBodyAsBytes();
        outputEvent.getHeaders().asMap().forEach((key, strings) -> {
            if (key.startsWith("Fn-Http-H-")) {
                String httpKey = key.substring("Fn-Http-H-".length());
                if (StringUtils.isNotEmpty(httpKey)) {
                    exchange.getResponseHeaders().put(httpKey, strings);
                }
            }
        });
        exchange.sendResponseHeaders(httpStatus.getCode(), bodyAsBytes.length);
        if (bodyAsBytes.length > 0) {
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(bodyAsBytes);
                responseBody.flush();
            }
        }
        exchange.close();
    }
}
