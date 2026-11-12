/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.oraclecloud.function.client;

import com.oracle.bmc.functions.FunctionsInvokeAsyncClient;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import io.micronaut.core.annotation.Internal;

/**
 * Supplies OCI Functions invoke clients bound to a function invoke endpoint.
 *
 * <p>OCI Functions are not invoked at the regional {@code functions} service endpoint the SDK client
 * builders default to, but at the invoke endpoint of each function. Implementations return a client
 * whose endpoint is the given invoke endpoint, so that functions with different invoke endpoints never
 * share a client whose endpoint is mutated between invocations.</p>
 *
 * @since 6.2.5
 */
@Internal
public interface OciFunctionInvokeClients {

    /**
     * @param invokeEndpoint The invoke endpoint of the function
     * @return A synchronous invoke client bound to the invoke endpoint
     */
    FunctionsInvokeClient syncClient(String invokeEndpoint);

    /**
     * @param invokeEndpoint The invoke endpoint of the function
     * @return An asynchronous invoke client bound to the invoke endpoint
     */
    FunctionsInvokeAsyncClient asyncClient(String invokeEndpoint);
}
