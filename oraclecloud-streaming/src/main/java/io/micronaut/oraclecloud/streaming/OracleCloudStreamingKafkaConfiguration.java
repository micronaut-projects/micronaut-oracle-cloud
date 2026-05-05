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

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.auth.sasl.InstancePrincipalsLoginModule;
import com.oracle.bmc.auth.sasl.OciMechanism;
import com.oracle.bmc.auth.sasl.ResourcePrincipalsLoginModule;
import com.oracle.bmc.auth.sasl.UserPrincipalsLoginModule;
import io.micronaut.configuration.kafka.config.KafkaDefaultConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;

import java.util.Properties;

/**
 * Configures Micronaut Kafka defaults for OCI Streaming.
 *
 * @since 6.0.0
 */
@Singleton
@Requires(classes = KafkaDefaultConfiguration.class)
@Requires(bean = OracleCloudStreamingConfiguration.class)
@Internal
public class OracleCloudStreamingKafkaConfiguration
    implements BeanCreatedEventListener<KafkaDefaultConfiguration>, Ordered {
    private static final int POSITION = Ordered.HIGHEST_PRECEDENCE + 100;
    private static final String STREAM_POOL_INTENT_PREFIX = "streamPoolId:";
    private static final String DEFAULT_SECOND_LEVEL_DOMAIN = "oraclecloud.com";
    private static final String BOOTSTRAP_SERVERS_TEMPLATE = "streaming.%s.oci.%s:9092";
    private static final String KAFKA_PREFIX = "kafka.";
    private static final String SECURITY_PROTOCOL = "SASL_SSL";
    private static final String SASL_PLAIN = "PLAIN";
    private static final String PLAIN_LOGIN_MODULE = "org.apache.kafka.common.security.plain.PlainLoginModule";
    private static final String MAX_REQUEST_SIZE = "1048576";
    private static final String RETRIES = "5";

    private final OracleCloudStreamingConfiguration streamingConfiguration;
    private final OracleCloudStreamingConfiguration.AuthTokenUsernameConfiguration authTokenUsernameConfiguration;
    private final RegionProvider regionProvider;
    private final Environment environment;

    /**
     * @param streamingConfiguration The OCI Streaming configuration
     * @param authTokenUsernameConfiguration The auth-token username parts configuration
     * @param regionProvider The OCI region provider
     * @param environment The environment
     */
    public OracleCloudStreamingKafkaConfiguration(OracleCloudStreamingConfiguration streamingConfiguration,
                                                  OracleCloudStreamingConfiguration.AuthTokenUsernameConfiguration authTokenUsernameConfiguration,
                                                  @Nullable RegionProvider regionProvider,
                                                  Environment environment) {
        this.streamingConfiguration = streamingConfiguration;
        this.authTokenUsernameConfiguration = authTokenUsernameConfiguration;
        this.regionProvider = regionProvider;
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return POSITION;
    }

    @Override
    public KafkaDefaultConfiguration onCreated(BeanCreatedEvent<KafkaDefaultConfiguration> event) {
        Properties kafkaProperties = event.getBean().getConfig();
        putIfNotConfigured(kafkaProperties, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, resolveBootstrapServers());
        putIfNotConfigured(kafkaProperties, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL);
        putIfNotConfigured(kafkaProperties, SaslConfigs.SASL_MECHANISM, resolveSaslMechanism());
        putIfNotConfigured(kafkaProperties, SaslConfigs.SASL_JAAS_CONFIG, resolveJaasConfig());
        putIfNotConfigured(kafkaProperties, ProducerConfig.RETRIES_CONFIG, RETRIES);
        putIfNotConfigured(kafkaProperties, ProducerConfig.MAX_REQUEST_SIZE_CONFIG, MAX_REQUEST_SIZE);
        putIfNotConfigured(kafkaProperties, ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, MAX_REQUEST_SIZE);
        return event.getBean();
    }

    private String resolveBootstrapServers() {
        if (StringUtils.isNotEmpty(streamingConfiguration.getBootstrapServers())) {
            return streamingConfiguration.getBootstrapServers();
        }
        Region ociRegion = resolveRegion();
        if (ociRegion == null) {
            throw new ConfigurationException("OCI Streaming requires either [" +
                OracleCloudStreamingConfiguration.PREFIX + ".bootstrap-servers] or an OCI region");
        }
        return String.format(
            BOOTSTRAP_SERVERS_TEMPLATE,
            ociRegion.getRegionId(),
            resolveSecondLevelDomain(ociRegion)
        );
    }

    private Region resolveRegion() {
        if (StringUtils.isNotEmpty(streamingConfiguration.getRegion())) {
            return Region.fromRegionCodeOrId(streamingConfiguration.getRegion());
        }
        if (regionProvider != null) {
            return regionProvider.getRegion();
        }
        return null;
    }

    private String resolveSecondLevelDomain(Region region) {
        if (region.getRealm() == null || StringUtils.isEmpty(region.getRealm().getSecondLevelDomain())) {
            return DEFAULT_SECOND_LEVEL_DOMAIN;
        }
        return region.getRealm().getSecondLevelDomain();
    }

    private String resolveSaslMechanism() {
        if (resolveAuthenticationMode() == OracleCloudStreamingConfiguration.AuthenticationMode.AUTH_TOKEN) {
            return SASL_PLAIN;
        }
        return OciMechanism.OCI_RSA_SHA256.mechanismName();
    }

    private String resolveJaasConfig() {
        return switch (resolveAuthenticationMode()) {
            case AUTH_TOKEN -> plainLoginModule();
            case INSTANCE_PRINCIPAL -> ociLoginModule(InstancePrincipalsLoginModule.class.getName(), metadataOption());
            case RESOURCE_PRINCIPAL -> ociLoginModule(ResourcePrincipalsLoginModule.class.getName(), "");
            case USER_PRINCIPAL -> ociLoginModule(UserPrincipalsLoginModule.class.getName(), userPrincipalOptions());
        };
    }

    private OracleCloudStreamingConfiguration.AuthenticationMode resolveAuthenticationMode() {
        OracleCloudStreamingConfiguration.AuthenticationMode authMode = streamingConfiguration.getAuthMode();
        if (authMode != null) {
            return authMode;
        }
        if (StringUtils.isNotEmpty(streamingConfiguration.getAuthToken())) {
            return OracleCloudStreamingConfiguration.AuthenticationMode.AUTH_TOKEN;
        }
        return OracleCloudStreamingConfiguration.AuthenticationMode.USER_PRINCIPAL;
    }

    private String plainLoginModule() {
        String username = resolveUsername();
        String authToken = streamingConfiguration.getAuthToken();
        if (StringUtils.isEmpty(authToken)) {
            throw new ConfigurationException("OCI Streaming auth-token mode requires [" +
                OracleCloudStreamingConfiguration.PREFIX + ".auth-token]");
        }
        return PLAIN_LOGIN_MODULE + " required username=\"" + escapeJaas(username) +
            "\" password=\"" + escapeJaas(authToken) + "\";";
    }

    private String resolveUsername() {
        if (StringUtils.isNotEmpty(streamingConfiguration.getUsername())) {
            return streamingConfiguration.getUsername();
        }
        if (StringUtils.isEmpty(streamingConfiguration.getTenancyName()) ||
            StringUtils.isEmpty(authTokenUsernameConfiguration.getUserName())) {
            throw new ConfigurationException("OCI Streaming auth-token mode requires either [" +
                OracleCloudStreamingConfiguration.PREFIX + ".username] or both [" +
                OracleCloudStreamingConfiguration.PREFIX + ".tenancy-name] and [" +
                OracleCloudStreamingConfiguration.PREFIX + ".user-name]");
        }
        if (StringUtils.isNotEmpty(streamingConfiguration.getDomainName())) {
            return streamingConfiguration.getTenancyName() + "/" +
                streamingConfiguration.getDomainName() + "/" +
                authTokenUsernameConfiguration.getUserName() + "/" +
                streamingConfiguration.getStreamPoolId();
        }
        return streamingConfiguration.getTenancyName() + "/" +
            authTokenUsernameConfiguration.getUserName() + "/" +
            streamingConfiguration.getStreamPoolId();
    }

    private String ociLoginModule(String loginModule, String options) {
        StringBuilder builder = new StringBuilder(loginModule)
            .append(" required intent=\"")
            .append(escapeJaas(STREAM_POOL_INTENT_PREFIX + streamingConfiguration.getStreamPoolId()))
            .append('"');
        if (StringUtils.isNotEmpty(options)) {
            builder.append(' ').append(options);
        }
        return builder.append(';').toString();
    }

    private String metadataOption() {
        if (StringUtils.isEmpty(streamingConfiguration.getMetadataBaseUrl())) {
            return "";
        }
        return "metadataBaseUrl=\"" + escapeJaas(streamingConfiguration.getMetadataBaseUrl()) + "\"";
    }

    private String userPrincipalOptions() {
        StringBuilder options = new StringBuilder();
        appendJaasOption(options, "config", streamingConfiguration.getConfigFile());
        appendJaasOption(options, "profile", streamingConfiguration.getProfile());
        return options.toString();
    }

    private static void appendJaasOption(StringBuilder options, String name, String value) {
        if (StringUtils.isNotEmpty(value)) {
            if (!options.isEmpty()) {
                options.append(' ');
            }
            options.append(name).append("=\"").append(escapeJaas(value)).append('"');
        }
    }

    private void putIfNotConfigured(Properties properties, String key, String value) {
        if (!environment.containsProperty(KAFKA_PREFIX + key)) {
            properties.setProperty(key, value);
        }
    }

    private static String escapeJaas(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
