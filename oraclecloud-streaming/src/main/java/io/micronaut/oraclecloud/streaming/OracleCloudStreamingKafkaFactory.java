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
package io.micronaut.oraclecloud.streaming;

import io.micronaut.configuration.kafka.config.KafkaDefaultConfiguration;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;

/**
 * Creates Kafka configuration when OCI Streaming is configured without a separate {@code kafka.*} section.
 *
 * @since 6.0.0
 */
@Factory
@Requires(classes = KafkaDefaultConfiguration.class)
@Requires(bean = OracleCloudStreamingConfiguration.class)
@Requires(property = "kafka.enabled", notEquals = StringUtils.FALSE)
final class OracleCloudStreamingKafkaFactory {

    @Singleton
    @Requires(missingBeans = KafkaDefaultConfiguration.class)
    KafkaDefaultConfiguration kafkaDefaultConfiguration(Environment environment) {
        return new KafkaDefaultConfiguration(environment);
    }
}
