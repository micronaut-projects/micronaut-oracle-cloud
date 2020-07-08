package io.micronaut.oci.objectstorage;

import com.oracle.bmc.apigateway.GatewayClient;
import com.oracle.bmc.filestorage.FileStorageClient;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// test that each SDK client is setup and injectable
@MicronautTest
public class SdkTest {
    @Test
    void testFileStorageClient(FileStorageClient client) {
        Assertions.assertNotNull(client);
    }

    @Test
    void testApiGatewayClient(GatewayClient client) {
        Assertions.assertNotNull(client);
    }
}
