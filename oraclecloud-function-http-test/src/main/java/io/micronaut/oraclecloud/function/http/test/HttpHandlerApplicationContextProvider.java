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

import com.sun.net.httpserver.HttpHandler;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextProvider;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;

/**
 * Implementation of {@link ApplicationContextProvider} that provides an {@link HttpHandler} and an {@link ApplicationContext}.
 */
@Experimental
@Internal
public class HttpHandlerApplicationContextProvider implements ApplicationContextProvider {
    private final HttpHandler httpHandler;
    private final ApplicationContext applicationContext;

    HttpHandlerApplicationContextProvider(HttpHandler httpHandler,
                                          ApplicationContext applicationContext) {
        this.httpHandler = httpHandler;
        this.applicationContext = applicationContext;
    }

    @Override
    public @NonNull ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Returns the {@link HttpHandler}.
     *
     * @return the handler
     */
    public HttpHandler getHttpHandler() {
        return httpHandler;
    }
}
