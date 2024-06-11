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

import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.FunctionRuntimeContext;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.tck.ServerUnderTest;
import io.micronaut.oraclecloud.function.http.HttpFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OracleCloudFunctionServerUnderTest implements ServerUnderTest {

    public static final String ORACLE_CLOUD_ENVIRONMENT = "oraclecloud";

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudFunctionServerUnderTest.class);

    private HttpFunction function;
    private ApplicationContext applicationContext;
    private RuntimeContext functionContext;

    public OracleCloudFunctionServerUnderTest(Map<String, Object> properties) {
        properties.put("micronaut.server.context-path", "/");
        properties.put("endpoints.health.service-ready-indicator-enabled", StringUtils.FALSE);
        properties.put("endpoints.refresh.enabled", StringUtils.FALSE);
        this.applicationContext = ApplicationContext
            .builder(Environment.FUNCTION, ORACLE_CLOUD_ENVIRONMENT, Environment.TEST)
            .eagerInitConfiguration(true)
            .eagerInitSingletons(true)
            .properties(properties)
            .deduceEnvironment(false)
            .start();
        this.function = new HttpFunction(applicationContext);
        this.functionContext = new FunctionRuntimeContext(null, new HashMap<>());
        this.function.setupContext(this.functionContext);
    }

    @Override
    public <I, O> HttpResponse<O> exchange(HttpRequest<I> request, Argument<O> bodyType) {
        InputEvent inputEvent = FnInputEventFactory.create(request);
        FnHttpGatewayContextAdapter context = new FnHttpGatewayContextAdapter(request, inputEvent);
        OutputEvent outputEvent = function.handleRequest(context, inputEvent);
        HttpResponse<O> response = new FnOutputEventAdapter<>(outputEvent, context, function.getApplicationContext().getBean(ConversionService.class));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Response status: {}", response.getStatus());
        }
        if (response.getStatus().getCode() >= 400) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Response body: {}", response.getBody(String.class));
            }
            throw new HttpClientResponseException("error " + response.getStatus().getReason() + " (" + response.getStatus().getCode() + ")", response);
        }
        return response;
    }

    @Override
    public <I, O, E> HttpResponse<O> exchange(HttpRequest<I> request, Argument<O> bodyType, Argument<E> errorType) {
        return exchange(request, bodyType);
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return function.getApplicationContext();
    }

    @Override
    public Optional<Integer> getPort() {
        return Optional.of(1234);
    }

    @Override
    public void close() throws IOException {
        applicationContext.close();
        function.close();
    }
}
