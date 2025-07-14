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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.ssl.AbstractClientSslConfiguration;
import io.micronaut.http.ssl.ClientSslConfiguration;
import io.micronaut.http.ssl.SslConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Objects;

/**
 * Default configuration for the OCI SDK clients.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@ConfigurationProperties(OracleCloudCoreFactory.ORACLE_CLOUD + ".client")
@BootstrapContextCompatible
@Named(OracleCloudCoreFactory.ORACLE_CLOUD)
public class OracleCloudClientConfigurationProperties extends AbstractOracleCloudClientConfigurationProperties {

    private final OracleCloudClientConnectionPoolConfiguration connectionPoolConfiguration;
    private final OracleCloudClientWebSocketCompressionConfiguration webSocketCompressionConfiguration;
    private final OracleCloudClientHttp2ClientConfiguration http2Configuration;

    public OracleCloudClientConfigurationProperties(
        @Nullable OracleCloudClientConnectionPoolConfiguration connectionPoolConfiguration,
        @Nullable OracleCloudClientWebSocketCompressionConfiguration webSocketCompressionConfiguration,
        @Nullable OracleCloudClientHttp2ClientConfiguration http2Configuration,
        @Nullable OracleCloudClientSslClientConfiguration sslConfiguration,
        DefaultHttpClientConfiguration defaultHttpClientConfiguration
    ) {
        super(defaultHttpClientConfiguration);
        if (sslConfiguration != null) {
            setSslConfiguration(sslConfiguration);
        }
        this.connectionPoolConfiguration = Objects.requireNonNullElseGet(connectionPoolConfiguration, OracleCloudClientConnectionPoolConfiguration::new);
        this.webSocketCompressionConfiguration = Objects.requireNonNullElseGet(webSocketCompressionConfiguration, OracleCloudClientWebSocketCompressionConfiguration::new);
        this.http2Configuration = Objects.requireNonNullElseGet(http2Configuration, OracleCloudClientHttp2ClientConfiguration::new);
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
    public OracleCloudClientHttp2ClientConfiguration getHttp2Configuration() {
        return http2Configuration;
    }

    /**
     * The default connection pool configuration.
     */
    @ConfigurationProperties(ConnectionPoolConfiguration.PREFIX)
    public static class OracleCloudClientConnectionPoolConfiguration extends ConnectionPoolConfiguration {
    }

    /**
     * The default WebSocket compression configuration.
     */
    @ConfigurationProperties(WebSocketCompressionConfiguration.PREFIX)
    public static class OracleCloudClientWebSocketCompressionConfiguration extends WebSocketCompressionConfiguration {
    }

    /**
     * The service HTTP/2 configuration.
     */
    @ConfigurationProperties(WebSocketCompressionConfiguration.PREFIX)
    public static class OracleCloudClientHttp2ClientConfiguration extends Http2ClientConfiguration {
    }

    /**
     * The default connection pool configuration.
     */
    @ConfigurationProperties("ssl")
    public static class OracleCloudClientSslClientConfiguration extends AbstractClientSslConfiguration {

        @Inject
        public OracleCloudClientSslClientConfiguration(ClientSslConfiguration clientSslConfiguration) {
            this.setEnabled(true);
            this.readExisting(clientSslConfiguration, clientSslConfiguration.getKey(), clientSslConfiguration.getKeyStore(), clientSslConfiguration.getTrustStore());
            this.setInsecureTrustAllCertificates(clientSslConfiguration.isEnabled());
        }

        public OracleCloudClientSslClientConfiguration() {
            this.setEnabled(true);
        }

        /**
         * Sets the key configuration.
         *
         * @param keyConfiguration The key configuration.
         */
        void setKey(@Nullable OracleCloudClientSslClientConfiguration.DefaultKeyConfiguration keyConfiguration) {
            if (keyConfiguration != null) {
                super.setKey(keyConfiguration);
            }
        }

        /**
         * Sets the key store.
         *
         * @param keyStoreConfiguration The key store configuration
         */
        void setKeyStore(@Nullable OracleCloudClientSslClientConfiguration.DefaultKeyStoreConfiguration keyStoreConfiguration) {
            if (keyStoreConfiguration != null) {
                super.setKeyStore(keyStoreConfiguration);
            }
        }

        /**
         * Sets trust store configuration.
         *
         * @param trustStore The trust store configuration
         */
        void setTrustStore(@Nullable OracleCloudClientSslClientConfiguration.DefaultTrustStoreConfiguration trustStore) {
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
