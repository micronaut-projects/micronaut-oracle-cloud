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
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.DefaultMutableConversionService;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.context.ServerHttpRequestContext;
import io.micronaut.oraclecloud.function.OciFunction;
import io.micronaut.servlet.http.DefaultServletExchange;
import io.micronaut.servlet.http.ServletExchange;
import io.micronaut.servlet.http.ServletHttpHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * A parent HttpFunction for authoring Project.fn gateway functions.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Singleton
@ReflectiveAccess
public class HttpFunction extends OciFunction {
    private ServletHttpHandler<InputEvent, OutputEvent> httpHandler;

    private final ConversionService conversionService;

    /**
     * Default constructor.
     */
    public HttpFunction() {
        this.conversionService = new DefaultMutableConversionService();
    }

    /**
     * Constructor for using a shared application context.
     * @param applicationContext The application context
     */
    @Inject
    protected HttpFunction(ApplicationContext applicationContext) {
        super(applicationContext);
        this.conversionService = applicationContext.getConversionService();
    }

    @Override
    protected final void setup(RuntimeContext ctx) {
        this.httpHandler = new ServletHttpHandler<>(getApplicationContext(), conversionService) {
            @Override
            protected ServletExchange<InputEvent, OutputEvent> createExchange(InputEvent request, OutputEvent response) {
                throw new UnsupportedOperationException("Use handleRequest to invoke the function");
            }
        };
        setupGateway(ctx);
    }

    /**
     * Method that subclasses can override to customize gateway setup.
     * @param ctx The context
     */
    protected void setupGateway(@NonNull RuntimeContext ctx) {
        // no-op
    }

    /**
     * @return The HTTP handler.
     */
    public final @NonNull ServletHttpHandler<InputEvent, OutputEvent> getHttpHandler() {
        return httpHandler;
    }

    /**
     * Main entry point for Gateway functions for Project.fn.
     *
     * @param gatewayContext The gateway context
     * @param inputEvent The input event
     * @return The output event
     */
    @SuppressWarnings("unused")
    @ReflectiveAccess
    public OutputEvent handleRequest(HTTPGatewayContext gatewayContext, InputEvent inputEvent) {
        FnServletResponse<Object> response = new FnServletResponse<>(gatewayContext, conversionService);
        FnServletRequest<Object> servletRequest = new FnServletRequest<>(inputEvent, response, gatewayContext, conversionService, httpHandler.getMediaTypeCodecRegistry());
        DefaultServletExchange<InputEvent, OutputEvent> exchange = new DefaultServletExchange<>(
            servletRequest,
                response
        );
        try (PropagatedContext.Scope ignore = PropagatedContext.getOrEmpty().plus(new ServerHttpRequestContext(servletRequest)).propagate()) {
            this.httpHandler.service(
                exchange
            );
            return response.getNativeResponse();
        }
    }
}
