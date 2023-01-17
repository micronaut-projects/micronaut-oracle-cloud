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

import com.fnproject.fn.api.RuntimeContext;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.bind.DefaultRequestBinderRegistry;
import io.micronaut.http.bind.binders.RequestArgumentBinder;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;
import io.micronaut.http.codec.MediaTypeCodecRegistry;
import io.micronaut.servlet.http.ServletBinderRegistry;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Internal
@Singleton
@Replaces(DefaultRequestBinderRegistry.class)
class FnBinderRegistry extends ServletBinderRegistry {

    private static final Argument<RuntimeContext> RUNTIME_CONTEXT_ARGUMENT = Argument.of(RuntimeContext.class);
    private final RuntimeContext runtimeContext;

    /**
     * Default constructor.
     *  @param mediaTypeCodecRegistry   The media type codec registry
     * @param conversionService         The conversion service
     * @param binders                   Any registered binders
     * @param runtimeContext            The runtime context.
     */
    public FnBinderRegistry(MediaTypeCodecRegistry mediaTypeCodecRegistry,
                            ConversionService conversionService,
                            List<RequestArgumentBinder> binders,
                            RuntimeContext runtimeContext) {
        super(mediaTypeCodecRegistry, conversionService, binders);
        this.runtimeContext = runtimeContext;

        this.byAnnotation.put(Body.class, new FnBodyBinder<>(conversionService, mediaTypeCodecRegistry));
        this.byType.put(RuntimeContext.class, new FnRuntimeContextBinder());
    }

    private final class FnRuntimeContextBinder implements TypedRequestArgumentBinder<RuntimeContext> {
        private final Optional<RuntimeContext> runtimeContext = Optional.ofNullable(FnBinderRegistry.this.runtimeContext);
        private final BindingResult<RuntimeContext> bindingResult = () -> runtimeContext;

        @Override
        public Argument<RuntimeContext> argumentType() {
            return RUNTIME_CONTEXT_ARGUMENT;
        }

        @Override
        public BindingResult<RuntimeContext> bind(ArgumentConversionContext<RuntimeContext> context, HttpRequest<?> source) {
            return bindingResult;
        }
    }
}
