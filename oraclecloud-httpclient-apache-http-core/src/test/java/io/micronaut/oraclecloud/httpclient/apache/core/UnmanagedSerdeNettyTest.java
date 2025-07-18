package io.micronaut.oraclecloud.httpclient.apache.core;

import com.oracle.bmc.http.client.HttpProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class UnmanagedSerdeNettyTest extends ApacheNettyTest {
    @Override
    HttpProvider provider() {
        return new ApacheCoreHttpProvider(new SerdeSerializer(), null);
    }

    @Override
    @Disabled("Disabled in subclass")
    @Test
    public void fullSetupTestManagedCustomProperties() {
        // Test is for only managed clients
    }
}
