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
