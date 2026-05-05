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

import io.micronaut.configuration.kafka.config.KafkaHealthConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

/**
 * Configures Kafka health for OCI Streaming's restricted Kafka API.
 *
 * @since 6.0.0
 */
@Singleton
@Requires(classes = KafkaHealthConfigurationProperties.class)
@Requires(bean = OracleCloudStreamingConfiguration.class)
@Internal
public class OracleCloudStreamingKafkaHealthConfiguration
    implements BeanCreatedEventListener<KafkaHealthConfigurationProperties> {
    private static final String KAFKA_HEALTH_RESTRICTED = "kafka.health.restricted";

    private final Environment environment;

    /**
     * @param environment The environment
     */
    public OracleCloudStreamingKafkaHealthConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Override
    public KafkaHealthConfigurationProperties onCreated(
        BeanCreatedEvent<KafkaHealthConfigurationProperties> event) {
        KafkaHealthConfigurationProperties bean = event.getBean();
        if (!environment.containsProperty(KAFKA_HEALTH_RESTRICTED)) {
            bean.setRestricted(true);
        }
        return bean;
    }
}
