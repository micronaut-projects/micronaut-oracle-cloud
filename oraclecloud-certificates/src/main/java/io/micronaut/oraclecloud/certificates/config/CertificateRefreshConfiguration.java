/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.oraclecloud.certificates.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import jakarta.annotation.Nullable;

import java.time.Duration;

/**
 * Configuration for certificate refresh frequency.
 *
 * @param frequency The frequency. Default 24 hours.
 * @param delay The initial delay. Default 24 hours.
 * @param retry The retry configuration.
 */
@ConfigurationProperties(OracleCloudCertificateProperties.PREFIX + ".refresh")
public record CertificateRefreshConfiguration(
    @Bindable(defaultValue = "24h")
    Duration frequency,
    @Bindable(defaultValue = "24h")
    Duration delay,
    @Nullable
    RetryConfiguration retry
) {
    /**
     * Retry retry configuration.
     * @param attempts Number of times to retry
     * @param delay The delay between retries.
     */
    @ConfigurationProperties("retry")
    record RetryConfiguration(
        @Bindable(defaultValue = "3")
        int attempts,
        @Bindable(defaultValue = "1s")
        Duration delay
    ) {}
}
