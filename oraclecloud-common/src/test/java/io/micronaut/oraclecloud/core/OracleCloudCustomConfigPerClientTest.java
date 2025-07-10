package io.micronaut.oraclecloud.core;

import com.oracle.bmc.ClientConfiguration;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
@Property(name = "oci.clients.identity.read-timeout-millis", value = "25000")
public class OracleCloudCustomConfigPerClientTest {

    @Test
    void testCustomConfig(
            @Named("identity") ClientConfiguration clientConfiguration,
            @Named("identity") AbstractOracleCloudClientConfigurationProperties abstractOracleCloudClientConfigurationProperties,
            ClientConfiguration defaultClientConfiguration,
            OracleCloudCoreFactory factory) {
        assertNotNull(clientConfiguration);
        assertNotEquals(25000, defaultClientConfiguration.getReadTimeoutMillis());
        assertEquals(25000, clientConfiguration.getReadTimeoutMillis());
        assertEquals(clientConfiguration.getReadTimeoutMillis(), abstractOracleCloudClientConfigurationProperties.getReadTimeout().get().toMillis());
        assertNotNull(abstractOracleCloudClientConfigurationProperties.getRetryDelayStrategy());
    }
}
