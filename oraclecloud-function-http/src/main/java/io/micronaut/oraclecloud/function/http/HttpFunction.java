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
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.oraclecloud.function.OciFunction;
import io.micronaut.servlet.http.DefaultServletExchange;
import io.micronaut.servlet.http.ServletExchange;
import io.micronaut.servlet.http.ServletHttpHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * An parent HttpFunction for authoring Project.fn gateway functions.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Singleton
public class HttpFunction extends OciFunction {
    private ServletHttpHandler<InputEvent, OutputEvent> httpHandler;

    /**
     * Default constructor.
     */
    @ReflectiveAccess
    public HttpFunction() {
    }

    /**
     * Constructor for using a shared application context.
     * @param applicationContext The application context
     */
    @Inject
    protected HttpFunction(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    protected final void setup(RuntimeContext ctx) {
        this.httpHandler = new ServletHttpHandler<InputEvent, OutputEvent>(getApplicationContext()) {
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
        FnServletResponse<Object> response = new FnServletResponse<>(gatewayContext);
        DefaultServletExchange<InputEvent, OutputEvent> exchange = new DefaultServletExchange<>(
                new FnServletRequest<>(inputEvent, response, gatewayContext, httpHandler.getMediaTypeCodecRegistry()),
                response
        );
        this.httpHandler.service(
                exchange
        );
        return response.getNativeResponse();
    }
}
