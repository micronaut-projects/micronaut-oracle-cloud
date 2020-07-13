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
package io.micronaut.oci.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.annotation.ReflectiveAccess;


/**
 * Parent class that can be used for Oracle Cloud functions.
 *
 * @author graemerocher
 * @since 1.0.0
 */
public abstract class OciFunction implements AutoCloseable {

    private ApplicationContext applicationContext;

    /**
     * Default constructor. Uses a self managed application context.
     */
    @ReflectiveAccess
    public OciFunction() {
    }

    /**
     * Construct a function with the given context.
     * @param applicationContext The application context
     */
    @ReflectiveAccess
    public OciFunction(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Sets of the function and creates the application context.
     * @param ctx The application context.
     */
    @FnConfiguration
    @ReflectiveAccess
    public final void setupContext(RuntimeContext ctx) {
        if (applicationContext == null) {
            applicationContext = newApplicationContextBuilder(ctx)
                    .build()
                    .start();
        }
        applicationContext.inject(this);
        if (enableSharedJackson()) {
            ctx.setAttribute(
                    "com.fnproject.fn.runtime.coercion.jackson.JacksonCoercion.om",
                    applicationContext.getBean(ObjectMapper.class));
        }
        setup(ctx);
    }

    /**
     * Setup method that can be overridden by users to add customizations.
     * @param ctx The runtime context
     */
    protected void setup(RuntimeContext ctx) {
        // no-op
    }

    /**
     * @return Whether Micronaut's shared Jackson object mapper should be used.
     */
    protected boolean enableSharedJackson() {
        return false;
    }

    /**
     * @return The application context.
     */
    public final @NonNull ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("Function not configured. Call setUp first");
        }
        return applicationContext;
    }

    /**
     * Construct the application context with the given builder.
     * @param ctx The context
     * @return The builder
     */
    @NonNull
    protected ApplicationContextBuilder newApplicationContextBuilder(RuntimeContext ctx) {
        return ApplicationContext
                .builder(Environment.FUNCTION, Environment.ORACLE_CLOUD)
                .deduceEnvironment(false)
                .propertySources(PropertySource.of(
                        Environment.FUNCTION,
                        ctx.getConfiguration(),
                        PropertySource.PropertyConvention.ENVIRONMENT_VARIABLE
                ))
                .singletons(ctx);
    }

    @Override
    public void close() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
        }
    }
}
