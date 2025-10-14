package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.Serializer;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.oraclecloud.serde.OciSdkMicronautSerializer;
import io.micronaut.oraclecloud.serde.OciSerdeConfiguration;
import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManagedNettyIsDefaultSetTest {

    @Test
    void oke() {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            // Create a managed provider, then immediately deregister it to simulate unmanaged scenario
            ManagedNettyHttpProvider managed = new ManagedNettyHttpProvider(
                DefaultHttpClient.builder().build(),
                executor,
                null
            );

            Serializer serializer = HttpProvider.getDefault().getSerializer();
            Assertions.assertSame(
                OciSdkMicronautSerializer.getDefaultSerializer(),
                serializer,
                "Expected default unmanaged serializer when OKE constructor used"
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void defaultOne() {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            // Create a managed provider, then immediately deregister it to simulate unmanaged scenario
            ManagedNettyHttpProvider managed = new ManagedNettyHttpProvider(
                null,
                executor,
                ObjectMapper.getDefault(),
                null,
                null,
                null,
                null
            );

            Serializer serializer = HttpProvider.getDefault().getSerializer();
            Assertions.assertNotSame(
                OciSdkMicronautSerializer.getDefaultSerializer(),
                serializer,
                "Expected different serializer when default constructor used"
            );
        } finally {
            executor.shutdownNow();
        }
    }
}
