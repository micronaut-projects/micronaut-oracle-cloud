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
package io.micronaut.oci.core;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.type.Argument;

import java.io.File;

/**
 * Allows enabling config based configuration.
 *
 * @author graemerocher
 * @since 1.0.0
 */
class OracleCloudConfigCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        String configPath = context.getProperty("oracle.cloud.config", Argument.STRING)
                .orElseGet(() -> System.getProperty("user.home") + "/.oci/config");
        if (new File(configPath).exists()) {
            return true;
        } else {
            context.fail("No Oracle Cloud Configuration found at path: " + configPath);
            return false;
        }
    }
}
