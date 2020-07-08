package io.micronaut.oci.objectstorage;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class ObjectStorageClientTest {

    private final ObjectStorageClient objectStorageClient;
    private final AuthenticationDetailsProvider detailsProvider;

    public ObjectStorageClientTest(
            ObjectStorageClient objectStorageClient,
            AuthenticationDetailsProvider detailsProvider) {
        this.objectStorageClient = objectStorageClient;
        this.detailsProvider = detailsProvider;
    }

    @Test
    void testObjectStorageClient() {
        final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
        builder.namespaceName("kg");
        builder.compartmentId(detailsProvider.getTenantId());
        final List<BucketSummary> items = objectStorageClient
                .listBuckets(builder.build())
                .getItems();

        assertNotNull(items);
    }
}
