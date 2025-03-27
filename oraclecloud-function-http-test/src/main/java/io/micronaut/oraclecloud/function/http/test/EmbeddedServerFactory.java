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
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * Factory for creating a {@link HttpHandler} and an {@link ApplicationContextProvider} with the {@code HttpServer} name qualifier.
 */
@Internal
@Experimental
@Factory
public class EmbeddedServerFactory {

    /**
     * @param applicationContext Application Context
     * @param testConfig Configuration map for fn.test.config
     * @return An {@link ApplicationContextProvider} with the {@code HttpServer} name qualifier.
     */
    @Named("HttpServer")
    @Singleton
    ApplicationContextProvider createFunction(ApplicationContext applicationContext,
                                              @Property(name = "fn.test.config")
                                              @MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.UNDER_SCORE_SEPARATED)
                                              Map<String, String> testConfig) {
        return new HttpHandlerApplicationContextProvider(
            new FnHttpExchange(testConfig, applicationContext.getEnvironment().getActiveNames()),
            applicationContext);
    }

    /**
     *
     * @param applicationContextProvider Application Context Provider
     * @return An {@link HttpHandler} instance
     */
    @Singleton
    HttpHandler createHandler(@Named("HttpServer") ApplicationContextProvider applicationContextProvider) {
        if (applicationContextProvider instanceof HttpHandlerApplicationContextProvider function) {
            return function.getHttpHandler();
        }
        throw new ConfigurationException("ApplicationContextProvider with name qualifier HttpServer should be of type HttpFunction");
    }
}
