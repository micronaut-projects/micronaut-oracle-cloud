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

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.retrier.RetryOptions;
import com.oracle.bmc.waiter.DelayStrategy;
import com.oracle.bmc.waiter.ExponentialBackoffDelayStrategy;
import com.oracle.bmc.waiter.FixedTimeDelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import com.oracle.bmc.waiter.MaxTimeTerminationStrategy;
import com.oracle.bmc.waiter.TerminationStrategy;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.ssl.ClientSslConfiguration;
import io.micronaut.http.ssl.SslConfiguration;

import java.time.Duration;

/**
 * Configuration for the {@link com.oracle.bmc.auth.AuthenticationDetailsProvider}.
 *
 * @since 5.2.1
 */
public abstract class AbstractOracleCloudClientConfigurationProperties extends HttpClientConfiguration {

    @ConfigurationBuilder(prefixes = "", excludes = {"retryConfiguration", "circuitBreakerConfiguration", "circuitBreaker", "readTimeoutMillis"})
    private final ClientConfiguration.ClientConfigurationBuilder clientBuilder = ClientConfiguration.builder();

    private final RetryConfiguration.Builder retryBuilder = RetryConfiguration.builder();

    @ConfigurationBuilder(prefixes = "", value = "circuit-breaker")
    @Nullable
    private CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder circuitBreakerBuilder;

    @ConfigurationBuilder(value = "retry-termination-strategy")
    private TerminationStrategyConfig retryTerminationStrategy = new TerminationStrategyConfig();

    @ConfigurationBuilder(value = "retry-delay-strategy")
    private DelayStrategyConfig retryDelayStrategy = new DelayStrategyConfig();

    @ConfigurationBuilder(value = "retry-options")
    private RetryOptionsConfig retryOptionsConfig = new RetryOptionsConfig();

    @ConfigurationBuilder(value = "ssl.key-store")
    private SslConfiguration.KeyStoreConfiguration keyStoreConfiguration;

    @ConfigurationBuilder(value = "ssl.trust-store")
    private SslConfiguration.TrustStoreConfiguration trustStoreConfiguration;

    @ConfigurationBuilder(value = "ssl")
    private ClientSslConfiguration clientSslConfiguration;

    @ConfigurationBuilder(value = "pool")
    private DefaultHttpClientConfiguration.DefaultConnectionPoolConfiguration connectionPoolConfiguration = new DefaultHttpClientConfiguration.DefaultConnectionPoolConfiguration();

    public AbstractOracleCloudClientConfigurationProperties(DefaultHttpClientConfiguration httpClientConfiguration) {
        super();
        clientSslConfiguration = (ClientSslConfiguration) getSslConfiguration();
        if (httpClientConfiguration.getSslConfiguration() != null) {
            if (httpClientConfiguration.getSslConfiguration() instanceof ClientSslConfiguration defaultClientSslConfiguration) {
                clientSslConfiguration.setInsecureTrustAllCertificates(defaultClientSslConfiguration.isInsecureTrustAllCertificates());
            }
        }
        keyStoreConfiguration = getSslConfiguration().getKeyStore();
        trustStoreConfiguration = getSslConfiguration().getTrustStore();
    }

    /**
     * @return {@link ClientSslConfiguration}.
     */
    public ClientSslConfiguration getClientSslConfiguration() {
        return clientSslConfiguration;
    }

    /**
     * @param clientSslConfiguration the client ssl configuration.
     */
    public void setClientSslConfiguration(ClientSslConfiguration clientSslConfiguration) {
        this.clientSslConfiguration = clientSslConfiguration;
    }

    /**
     * @return {@link ConnectionPoolConfiguration}.
     */
    @Override
    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return connectionPoolConfiguration;
    }

    /**
     * @param connectionPoolConfiguration the connection pool configuration.
     */
    public void setConnectionPoolConfiguration(DefaultHttpClientConfiguration.DefaultConnectionPoolConfiguration connectionPoolConfiguration) {
        this.connectionPoolConfiguration = connectionPoolConfiguration;
    }

    /**
     * @return {@link SslConfiguration.KeyStoreConfiguration}.
     */
    public SslConfiguration.KeyStoreConfiguration getKeyStoreConfiguration() {
        return keyStoreConfiguration;
    }

    /**
     * @param keyStoreConfiguration the key store configuration.
     */
    public void setKeyStoreConfiguration(SslConfiguration.KeyStoreConfiguration keyStoreConfiguration) {
        this.keyStoreConfiguration = keyStoreConfiguration;
    }

    /**
     * @return {@link SslConfiguration.TrustStoreConfiguration}.
     */
    public SslConfiguration.TrustStoreConfiguration getTrustStoreConfiguration() {
        return trustStoreConfiguration;
    }

    /**
     * @param trustStoreConfiguration the trust store configuration.
     */
    public void setTrustStoreConfiguration(SslConfiguration.TrustStoreConfiguration trustStoreConfiguration) {
        this.trustStoreConfiguration = trustStoreConfiguration;
    }

    /**
     * @return {@link RetryOptionsConfig}.
     */
    public RetryOptionsConfig getRetryOptions() {
        return retryOptionsConfig;
    }

    /**
     * @param retryOptions retryOptions
     */
    public void setRetryOptions(RetryOptionsConfig retryOptions) {
        this.retryOptionsConfig = retryOptions;
    }

    /**
     * @return {@link TerminationStrategyConfig}
     */
    public TerminationStrategyConfig getRetryTerminationStrategy() {
        return retryTerminationStrategy;
    }

    /**
     * @param retryTerminationStrategy the retry termination strategy.
     */
    public void setRetryTerminationStrategy(TerminationStrategyConfig retryTerminationStrategy) {
        this.retryTerminationStrategy = retryTerminationStrategy;
    }

    /**
     * @return {@link DelayStrategyConfig}
     */
    public DelayStrategyConfig getRetryDelayStrategy() {
        return retryDelayStrategy;
    }

    /**
     * @param retryDelayStrategy
     */
    public void setRetryDelayStrategy(DelayStrategyConfig retryDelayStrategy) {
        this.retryDelayStrategy = retryDelayStrategy;
    }

    /**
     * @param readTimeoutMillis set the readTimeoutMillis both in client builder and to micronaut client config.
     */
    public void setReadTimeoutMillis(Integer readTimeoutMillis) {
        setReadTimeout(Duration.ofMillis(readTimeoutMillis));
        clientBuilder.readTimeoutMillis(readTimeoutMillis);
    }

    /**
     * @param readTimeout set the readTimeout both in client builder and to micronaut client config.
     */
    @Override
    public void setReadTimeout(@Nullable Duration readTimeout) {
        super.setReadTimeout(readTimeout);
        if (readTimeout != null) {
            clientBuilder.readTimeoutMillis(readTimeout.toMillisPart());
        }
    }

    /**
     * @return Obtains the configuration builder.
     */
    public ClientConfiguration.ClientConfigurationBuilder getClientBuilder() {
        configureOracleCloudClientRetry();
        clientBuilder.retryConfiguration(retryBuilder.build());
        if (circuitBreakerBuilder != null) {
            clientBuilder.circuitBreakerConfiguration(circuitBreakerBuilder.build());
        }
        return clientBuilder;
    }

    /**
     * @return  The circuit breaker config
     */
    public CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder getCircuitBreakerBuilder() {
        if (circuitBreakerBuilder == null) {
            circuitBreakerBuilder = CircuitBreakerConfiguration.builder();
        }
        return circuitBreakerBuilder;
    }

    private void configureOracleCloudClientRetry() {
        TerminationStrategy terminationStrategy = createTerminationStrategy(getRetryTerminationStrategy());
        DelayStrategy delayStrategy = createDelayStrategy(getRetryDelayStrategy());
        RetryOptions retryOptions = createRetryOptions(getRetryOptions());

        if (terminationStrategy != null) {
            retryBuilder.terminationStrategy(terminationStrategy);
        }

        if (delayStrategy != null) {
            retryBuilder.delayStrategy(delayStrategy);
        }

        if (retryOptions != null) {
            retryBuilder.retryOptions(retryOptions);
        }
    }

    @Nullable
    private TerminationStrategy createTerminationStrategy(TerminationStrategyConfig cfg) {
        if (cfg.getMaxTimeMillis() != null) {
            return new MaxTimeTerminationStrategy(cfg.getMaxTimeMillis());
        }
        if (cfg.getMaxAttempts() != null) {
            return new MaxAttemptsTerminationStrategy(cfg.getMaxAttempts());
        }
        return null;
    }

    @Nullable
    private DelayStrategy createDelayStrategy(DelayStrategyConfig cfg) {
        if (cfg.getTimeBetweenAttemptsInMillis() != null) {
            return new ExponentialBackoffDelayStrategy(cfg.getTimeBetweenAttemptsInMillis());
        }
        if (cfg.getMaxDelayMillis() != null) {
            return new FixedTimeDelayStrategy(cfg.getMaxDelayMillis());
        }

        return null;
    }

    @Nullable
    private RetryOptions createRetryOptions(RetryOptionsConfig cfg) {
        if (cfg.getMarkReadLimit() != null) {
            return new RetryOptions(cfg.getMarkReadLimit());
        }
        return null;
    }

    /**
     * Configuration holder for the {@link TerminationStrategy}.
     */
    public static final class TerminationStrategyConfig {
        private Integer maxAttempts;
        private Long maxTimeMillis;

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Long getMaxTimeMillis() {
            return maxTimeMillis;
        }

        public void setMaxTimeMillis(Long maxTimeMillis) {
            this.maxTimeMillis = maxTimeMillis;
        }
    }

    /**
     * Configuration holder for the {@link DelayStrategy}.
     */
    public static final class DelayStrategyConfig {
        private Long timeBetweenAttemptsInMillis;
        private Long maxDelayMillis;

        public Long getTimeBetweenAttemptsInMillis() {
            return timeBetweenAttemptsInMillis;
        }

        public void setTimeBetweenAttemptsInMillis(Long timeBetweenAttemptsInMillis) {
            this.timeBetweenAttemptsInMillis = timeBetweenAttemptsInMillis;
        }

        public Long getMaxDelayMillis() {
            return maxDelayMillis;
        }

        public void setMaxDelayMillis(Long v) {
            this.maxDelayMillis = v;
        }
    }

    /**
     * Configuration holder for the {@link RetryOptions}.
     */
    public static final class RetryOptionsConfig {
        private Integer markReadLimit;

        public Integer getMarkReadLimit() {
            return markReadLimit;
        }

        public void setMarkReadLimit(Integer markReadLimit) {
            this.markReadLimit = markReadLimit;
        }
    }
}
