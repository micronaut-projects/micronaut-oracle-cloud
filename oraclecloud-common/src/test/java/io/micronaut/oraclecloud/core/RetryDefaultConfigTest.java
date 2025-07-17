package io.micronaut.oraclecloud.core;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.retrier.RetryConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class RetryDefaultConfigTest {

    @Test
    void testRetryDefaultConfig(
        ClientConfiguration clientConfiguration,
        OracleCloudCoreFactory factory) {
        assertNotNull(clientConfiguration);
        RetryConfiguration retryConfiguration = clientConfiguration.getRetryConfiguration();
        assertEquals(retryConfiguration.getRetryCondition(), RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION.getRetryCondition());
        assertEquals(retryConfiguration.getRetryOptions(), RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION.getRetryOptions());
        assertEquals(retryConfiguration.getTerminationStrategy(), RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION.getTerminationStrategy());
        assertEquals(retryConfiguration.getDelayStrategy(), RetryConfiguration.SDK_DEFAULT_RETRY_CONFIGURATION.getDelayStrategy());
    }
}
