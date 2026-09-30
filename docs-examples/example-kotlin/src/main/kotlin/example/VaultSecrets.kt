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
package example

import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Singleton
class VaultSecrets {

    // tag::value[]
    @Value("\${SECRET_ONE}") lateinit var secretOne: String
    // end::value[]

    // tag::property[]
    @field:Property(name = "SECRET_TWO") lateinit var secretTwo: String
    // end::property[]
}
