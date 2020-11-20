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
import com.fnproject.fn.runtime.FunctionRuntimeContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;

import javax.inject.Singleton;
import java.util.Collections;

/**
 * Factory that creates a default runtime context if none is present.
 *
 * @author graemerocher
 * @since 1.1.1
 */
@Factory
@Internal
final class DefaultContextFactory {

    /**
     * Default runtime context.
     * @return The default context
     */
    @Singleton
    @Requires(missingBeans = RuntimeContext.class)
    RuntimeContext defaultContext() {
        return new FunctionRuntimeContext(
                null,
                Collections.emptyMap()
        );
    }
}
