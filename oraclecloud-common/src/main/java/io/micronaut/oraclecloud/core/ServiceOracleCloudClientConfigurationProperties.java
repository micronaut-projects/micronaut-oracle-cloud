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
package io.micronaut.oraclecloud.core;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.naming.Named;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.ssl.AbstractClientSslConfiguration;
import io.micronaut.http.ssl.SslConfiguration;

import java.util.Objects;

import static io.micronaut.oraclecloud.core.ServiceOracleCloudClientConfigurationProperties.PREFIX;

/**
 * Configuration for each of the OCI SDK clients.
 *
 * @since 5.3.0
 */
@EachProperty(PREFIX)
@BootstrapContextCompatible
public final class ServiceOracleCloudClientConfigurationProperties extends AbstractOracleCloudClientConfigurationProperties implements Named {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".clients";

    private String name;
    private final ServiceOracleCloudClientConnectionPoolConfiguration connectionPoolConfiguration;
    private final ServiceOracleCloudClientWebSocketCompressionConfiguration webSocketCompressionConfiguration;
    private final ServiceOracleCloudClientHttp2ClientConfiguration http2Configuration;

    public ServiceOracleCloudClientConfigurationProperties(@Parameter String name,
                                                           @Nullable ServiceOracleCloudClientConnectionPoolConfiguration connectionPoolConfiguration,
                                                           @Nullable ServiceOracleCloudClientWebSocketCompressionConfiguration webSocketCompressionConfiguration,
                                                           @Nullable ServiceOracleCloudClientHttp2ClientConfiguration http2Configuration,
                                                           @Nullable ServiceOracleCloudClientSslClientConfiguration sslConfiguration,
                                                           HttpClientConfiguration defaultHttpClientConfiguration
    ) {
        super(defaultHttpClientConfiguration);
        this.name = name;
        if (sslConfiguration != null) {
            setSslConfiguration(sslConfiguration);
        }
        this.connectionPoolConfiguration = Objects.requireNonNullElseGet(connectionPoolConfiguration, ServiceOracleCloudClientConnectionPoolConfiguration::new);
        this.webSocketCompressionConfiguration = Objects.requireNonNullElseGet(webSocketCompressionConfiguration, ServiceOracleCloudClientWebSocketCompressionConfiguration::new);
        this.http2Configuration = Objects.requireNonNullElseGet(http2Configuration, ServiceOracleCloudClientHttp2ClientConfiguration::new);
    }

    /**
     * @return the serviceId for a {@link Named} used by {@link jakarta.inject.Named}.
     */
    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return connectionPoolConfiguration;
    }

    @Override
    public WebSocketCompressionConfiguration getWebSocketCompressionConfiguration() {
        return webSocketCompressionConfiguration;
    }

    @Override
    public ServiceOracleCloudClientHttp2ClientConfiguration getHttp2Configuration() {
        return http2Configuration;
    }

    /**
     * The default connection pool configuration.
     */
    @ConfigurationProperties(ConnectionPoolConfiguration.PREFIX)
    public static class ServiceOracleCloudClientConnectionPoolConfiguration extends ConnectionPoolConfiguration {
    }

    /**
     * The default WebSocket compression configuration.
     */
    @ConfigurationProperties(WebSocketCompressionConfiguration.PREFIX)
    public static class ServiceOracleCloudClientWebSocketCompressionConfiguration extends WebSocketCompressionConfiguration {
    }

    /**
     * The service HTTP/2 configuration.
     */
    @ConfigurationProperties(WebSocketCompressionConfiguration.PREFIX)
    public static class ServiceOracleCloudClientHttp2ClientConfiguration extends Http2ClientConfiguration {
    }

    /**
     * The default connection pool configuration.
     */
    @ConfigurationProperties("ssl")
    public static class ServiceOracleCloudClientSslClientConfiguration extends AbstractClientSslConfiguration {

        /**
         * Sets the key configuration.
         *
         * @param keyConfiguration The key configuration.
         */
        void setKey(@Nullable ServiceOracleCloudClientSslClientConfiguration.DefaultKeyConfiguration keyConfiguration) {
            if (keyConfiguration != null) {
                super.setKey(keyConfiguration);
            }
        }

        /**
         * Sets the key store.
         *
         * @param keyStoreConfiguration The key store configuration
         */
        void setKeyStore(@Nullable ServiceOracleCloudClientSslClientConfiguration.DefaultKeyStoreConfiguration keyStoreConfiguration) {
            if (keyStoreConfiguration != null) {
                super.setKeyStore(keyStoreConfiguration);
            }
        }

        /**
         * Sets trust store configuration.
         *
         * @param trustStore The trust store configuration
         */
        void setTrustStore(@Nullable ServiceOracleCloudClientSslClientConfiguration.DefaultTrustStoreConfiguration trustStore) {
            if (trustStore != null) {
                super.setTrustStore(trustStore);
            }
        }

        /**
         * The default {@link io.micronaut.http.ssl.SslConfiguration.KeyConfiguration}.
         */
        @SuppressWarnings("WeakerAccess")
        @ConfigurationProperties(SslConfiguration.KeyConfiguration.PREFIX)
        public static class DefaultKeyConfiguration extends SslConfiguration.KeyConfiguration {
        }

        /**
         * The default {@link io.micronaut.http.ssl.SslConfiguration.KeyStoreConfiguration}.
         */
        @SuppressWarnings("WeakerAccess")
        @ConfigurationProperties(SslConfiguration.KeyStoreConfiguration.PREFIX)
        public static class DefaultKeyStoreConfiguration extends SslConfiguration.KeyStoreConfiguration {
        }

        /**
         * The default {@link io.micronaut.http.ssl.SslConfiguration.TrustStoreConfiguration}.
         */
        @SuppressWarnings("WeakerAccess")
        @ConfigurationProperties(SslConfiguration.TrustStoreConfiguration.PREFIX)
        public static class DefaultTrustStoreConfiguration extends SslConfiguration.TrustStoreConfiguration {
        }
    }
}
