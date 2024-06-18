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
import io.micronaut.core.util.SupplierUtil;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.body.CloseableByteBody;
import io.micronaut.http.context.ServerHttpRequestContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.oraclecloud.function.OciFunction;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.servlet.http.DefaultServletExchange;
import io.micronaut.servlet.http.ServletExchange;
import io.micronaut.servlet.http.ServletHttpHandler;
import io.micronaut.servlet.http.body.InputStreamByteBody;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.OptionalLong;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

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
    private final Supplier<Executor> ioExecutor = SupplierUtil.memoized(() -> getApplicationContext().getBean(Executor.class, Qualifiers.byName(TaskExecutors.BLOCKING)));

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
    HttpFunction(ApplicationContext applicationContext) {
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
        /*
         This is a bit tricky. fnproject only allows access to the body InputStream through this
         consumeBody method, which can only be called once and, after the lambda finishes, closes
         the stream. This is incompatible with the ByteBody architecture.

         To work around this limitation, we do a single big consumeBody around the entire request
         processing. This hopefully encompasses most users of the body stream. Any read operations
         inside this block are simply forwarded upstream.

         In case there *is* a downstream consumer that has not called allowDiscard and has not
         consumed all data yet, we call bufferIfNecessary. This call will buffer any remaining data
         so that this downstream consumer can continue reading.

         To prevent unnecessary buffering when there are no downstream consumers that require the
         (full) body, we take advantage of the allowDiscard mechanism. If InputStreamByteBody is
         never split, or all splits are closed or allowDiscarded, then closing the original
         InputStreamByteBody will also close the InputStream it wraps. This sets a flag in
         OptionalBufferingInputStream that disables buffering on bufferIfNecessary.
        */

        return inputEvent.consumeBody(stream -> {
            OptionalBufferingInputStream optionalBufferingInputStream = new OptionalBufferingInputStream(stream);
            OptionalLong contentLength = inputEvent.getHeaders().get(HttpHeaders.CONTENT_LENGTH).map(Long::parseLong).map(OptionalLong::of).orElse(OptionalLong.empty());
            try (CloseableByteBody body = InputStreamByteBody.create(optionalBufferingInputStream, contentLength, ioExecutor.get())) {

                FnServletRequest<Object> servletRequest = new FnServletRequest<>(
                    body, inputEvent, response, gatewayContext, conversionService,
                    httpHandler.getMediaTypeCodecRegistry()
                );
                DefaultServletExchange<InputEvent, OutputEvent> exchange = new DefaultServletExchange<>(
                    servletRequest,
                    response
                );
                try (PropagatedContext.Scope ignore = PropagatedContext.getOrEmpty().plus(new ServerHttpRequestContext(servletRequest)).propagate()) {
                    this.httpHandler.service(
                        exchange
                    );
                }
            }
            OutputEvent nativeResponse = response.getNativeResponse();
            optionalBufferingInputStream.bufferIfNecessary();
            return nativeResponse;
        });
    }
}
