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
package io.micronaut.oraclecloud.clients;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Internal Annotation to trigger the creation of SDK clients.
 */
@Retention(value = RetentionPolicy.SOURCE)
public @interface SdkClients {
    /**
     * @return The type of client to generate.
     */
    Kind value() default Kind.ASYNC;

    /**
     * @return The OCI SDK client class names to process.
     */
    String[] clientClasses() default {};

    /**
     * the type of client to generate.
     */
    enum Kind {
        ASYNC,
        REACTOR,
        RXJAVA2
    }
}
