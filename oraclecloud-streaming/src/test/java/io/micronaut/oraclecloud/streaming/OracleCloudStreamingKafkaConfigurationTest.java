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
import io.micronaut.configuration.kafka.config.KafkaHealthConfigurationProperties;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleCloudStreamingKafkaConfigurationTest {
    private static final String STREAM_POOL_ID = "ocid1.streampool.oc1.phx.example";

    @Test
    void kafkaConfigurationIsNotCreatedWithoutStreamingConfiguration() {
        try (ApplicationContext context = ApplicationContext.run(Map.of("spec.name", getClass().getSimpleName()))) {
            assertFalse(context.findBean(KafkaDefaultConfiguration.class).isPresent());
        }
    }

    @Test
    void kafkaConfigurationIsNotCreatedWhenStreamingIsDisabled() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.enabled", false,
            "oci.streaming.stream-pool-id", STREAM_POOL_ID
        ))) {
            assertFalse(context.findBean(OracleCloudStreamingConfiguration.class).isPresent());
            assertFalse(context.findBean(KafkaDefaultConfiguration.class).isPresent());
        }
    }

    @Test
    void kafkaConfigurationIsNotCreatedWhenKafkaIsDisabled() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "kafka.enabled", false,
            "oci.streaming.stream-pool-id", STREAM_POOL_ID
        ))) {
            assertTrue(context.findBean(OracleCloudStreamingConfiguration.class).isPresent());
            assertFalse(context.findBean(KafkaDefaultConfiguration.class).isPresent());
        }
    }

    @Test
    void configuresKafkaForAuthTokenStreaming() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.region", "us-phoenix-1",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.tenancy-name", "exampletenant",
            "oci.streaming.domain-name", "Default",
            "oci.streaming.user-name", "streamuser",
            "oci.streaming.auth-token", "secret"
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("streaming.us-phoenix-1.oci.oraclecloud.com:9092",
                properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("SASL_SSL", properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
            assertEquals("PLAIN", properties.getProperty(SaslConfigs.SASL_MECHANISM));
            assertEquals("org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"exampletenant/Default/streamuser/" + STREAM_POOL_ID + "\" password=\"secret\";",
                properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG));
            assertEquals("5", properties.getProperty(ProducerConfig.RETRIES_CONFIG));
            assertEquals("1048576", properties.getProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG));
            assertEquals("1048576", properties.getProperty(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG));
            assertTrue(context.getBean(KafkaHealthConfigurationProperties.class).isRestricted());
        }
    }

    @Test
    void configuresKafkaForFullAuthTokenUsername() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.username", "exampletenant/streamuser/" + STREAM_POOL_ID,
            "oci.streaming.auth-token", "secret"
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"exampletenant/streamuser/" + STREAM_POOL_ID + "\" password=\"secret\";",
                properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG));
        }
    }

    @Test
    void configuresBootstrapServersForRegionRealm() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.region", "us-gov-phoenix-1",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "resource-principal"
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("streaming.us-gov-phoenix-1.oci.oraclegovcloud.com:9092",
                properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        }
    }

    @Test
    void configuresKafkaForInstancePrincipals() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "instance-principal",
            "oci.streaming.metadata-base-url", "http://169.254.169.254/opc/v2"
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("streaming.example.com:9092", properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("OCI-RSA-SHA256", properties.getProperty(SaslConfigs.SASL_MECHANISM));
            assertEquals("com.oracle.bmc.auth.sasl.InstancePrincipalsLoginModule required " +
                    "intent=\"streamPoolId:" + STREAM_POOL_ID + "\" metadataBaseUrl=\"http://169.254.169.254/opc/v2\";",
                properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG));
        }
    }

    @Test
    void configuresKafkaForResourcePrincipals() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "resource-principal"
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("OCI-RSA-SHA256", properties.getProperty(SaslConfigs.SASL_MECHANISM));
            assertEquals("com.oracle.bmc.auth.sasl.ResourcePrincipalsLoginModule required " +
                    "intent=\"streamPoolId:" + STREAM_POOL_ID + "\";",
                properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG));
        }
    }

    @Test
    void configuresKafkaForUserPrincipals() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "user-principal",
            "oci.streaming.config-file", "/home/user/.oci/config",
            "oci.streaming.profile", "STREAMING"
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("OCI-RSA-SHA256", properties.getProperty(SaslConfigs.SASL_MECHANISM));
            assertEquals("com.oracle.bmc.auth.sasl.UserPrincipalsLoginModule required " +
                    "intent=\"streamPoolId:" + STREAM_POOL_ID + "\" " +
                    "config=\"/home/user/.oci/config\" profile=\"STREAMING\";",
                properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG));
        }
    }

    @Test
    void preservesExplicitKafkaHealthRestriction() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "kafka.health.restricted", false,
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "resource-principal"
        ))) {
            assertFalse(context.getBean(KafkaHealthConfigurationProperties.class).isRestricted());
        }
    }

    @Test
    void preservesExplicitKafkaProperties() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "kafka.bootstrap.servers", "custom.example.com:9092",
            "kafka.security.protocol", "PLAINTEXT",
            "kafka.sasl.mechanism", "CUSTOM",
            "kafka.sasl.jaas.config", "custom.LoginModule required;",
            "kafka.retries", "9",
            "kafka.max.request.size", "2097152",
            "kafka.max.partition.fetch.bytes", "3145728",
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "resource-principal"
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("custom.example.com:9092", properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("PLAINTEXT", properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
            assertEquals("CUSTOM", properties.getProperty(SaslConfigs.SASL_MECHANISM));
            assertEquals("custom.LoginModule required;", properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG));
            assertEquals("9", properties.getProperty(ProducerConfig.RETRIES_CONFIG));
            assertEquals("2097152", properties.getProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG));
            assertEquals("3145728", properties.getProperty(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG));
        }
    }

    @Test
    void preservesProgrammaticKafkaProperties() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "spec.name", "preserves-programmatic-kafka-properties",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID
        ))) {
            Properties properties = context.getBean(KafkaDefaultConfiguration.class).getConfig();

            assertEquals("programmatic.example.com:9092", properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
            assertEquals("PLAINTEXT", properties.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
            assertEquals("PROGRAMMATIC", properties.getProperty(SaslConfigs.SASL_MECHANISM));
            assertEquals("programmatic.LoginModule required;", properties.getProperty(SaslConfigs.SASL_JAAS_CONFIG));
            assertEquals("13", properties.getProperty(ProducerConfig.RETRIES_CONFIG));
            assertEquals("4194304", properties.getProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG));
            assertEquals("5242880", properties.getProperty(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG));
        }
    }

    @Test
    void failsWhenBootstrapServersAndRegionAreMissing() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "resource-principal"
        ))) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> context.getBean(KafkaDefaultConfiguration.class));

            Throwable cause = rootCause(exception);
            assertTrue(cause instanceof ConfigurationException);
            assertTrue(cause.getMessage().contains("bootstrap-servers"));
        }
    }

    @Test
    void failsWhenAuthTokenModeDoesNotHaveAuthToken() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-mode", "auth-token",
            "oci.streaming.username", "exampletenant/streamuser/" + STREAM_POOL_ID
        ))) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> context.getBean(KafkaDefaultConfiguration.class));

            Throwable cause = rootCause(exception);
            assertTrue(cause instanceof ConfigurationException);
            assertTrue(cause.getMessage().contains("auth-token"));
        }
    }

    @Test
    void failsWhenAuthTokenModeDoesNotHaveUsernameConfiguration() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "oci.streaming.bootstrap-servers", "streaming.example.com:9092",
            "oci.streaming.stream-pool-id", STREAM_POOL_ID,
            "oci.streaming.auth-token", "secret"
        ))) {
            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> context.getBean(KafkaDefaultConfiguration.class));

            Throwable cause = rootCause(exception);
            assertTrue(cause instanceof ConfigurationException);
            assertTrue(cause.getMessage().contains("username"));
        }
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Singleton
    @Requires(property = "spec.name", value = "preserves-programmatic-kafka-properties")
    static final class ProgrammaticKafkaConfiguration implements BeanCreatedEventListener<KafkaDefaultConfiguration>, Ordered {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public KafkaDefaultConfiguration onCreated(BeanCreatedEvent<KafkaDefaultConfiguration> event) {
            Properties properties = event.getBean().getConfig();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "programmatic.example.com:9092");
            properties.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
            properties.setProperty(SaslConfigs.SASL_MECHANISM, "PROGRAMMATIC");
            properties.setProperty(SaslConfigs.SASL_JAAS_CONFIG, "programmatic.LoginModule required;");
            properties.setProperty(ProducerConfig.RETRIES_CONFIG, "13");
            properties.setProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "4194304");
            properties.setProperty(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, "5242880");
            return event.getBean();
        }
    }
}
