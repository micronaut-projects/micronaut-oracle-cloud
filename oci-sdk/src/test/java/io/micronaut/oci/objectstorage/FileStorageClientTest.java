package io.micronaut.oci.objectstorage;

import com.oracle.bmc.filestorage.FileStorageClient;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class FileStorageClientTest {

    @Test
    void testFileStorageClient(FileStorageClient client) {
        Assertions.assertNotNull(client);
    }
}
