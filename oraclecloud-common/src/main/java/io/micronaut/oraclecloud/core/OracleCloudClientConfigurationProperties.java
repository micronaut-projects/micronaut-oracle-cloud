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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration for the {@link com.oracle.bmc.auth.AuthenticationDetailsProvider}.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@ConfigurationProperties(OracleCloudCoreFactory.ORACLE_CLOUD + ".client")
@BootstrapContextCompatible
public class OracleCloudClientConfigurationProperties {

    @ConfigurationBuilder(prefixes = "", excludes = {"retryConfiguration", "circuitBreakerConfiguration"})
    private final ClientConfiguration.ClientConfigurationBuilder clientBuilder = ClientConfiguration.builder();

    @ConfigurationBuilder(prefixes = "", value = "retry")
    @Nullable
    private RetryConfiguration.Builder retryBuilder;

    @ConfigurationBuilder(prefixes = "", value = "circuit-breaker")
    @Nullable
    private CircuitBreakerConfiguration.CircuitBreakerConfigurationBuilder circuitBreakerBuilder;

    /**
     * @return Obtains the configuration builder.
     */
    public ClientConfiguration.ClientConfigurationBuilder getClientBuilder() {
        if (retryBuilder != null) {
            clientBuilder.retryConfiguration(retryBuilder.build());
        }
        if (circuitBreakerBuilder != null) {
            clientBuilder.circuitBreakerConfiguration(circuitBreakerBuilder.build());
        }
        return clientBuilder;
    }

    /**
     * @return The retry config.
     */
    public RetryConfiguration.Builder getRetryBuilder() {
        if (retryBuilder == null) {
            retryBuilder = RetryConfiguration.builder();
        }
        return retryBuilder;
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

}
