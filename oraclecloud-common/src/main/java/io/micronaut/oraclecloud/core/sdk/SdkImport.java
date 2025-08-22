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
package io.micronaut.oraclecloud.core.sdk;

/**
 * Annotation used to import an SDK if it is generated with OCI SDK v3, but without Micronaut compatibility.
 */
public @interface SdkImport {
    /**
     * The type of the client. This should reference a type that extends from either
     * {@link com.oracle.bmc.http.internal.BaseAsyncClient} or {@link com.oracle.bmc.http.internal.BaseSyncClient}.
     *
     * @return The type of a client to import.
     */
    Class<?> value();
}
