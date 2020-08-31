package io.micronaut.oci.function;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Singleton
public class ObjectStorageFunction extends OciFunction {

    @Inject
    ObjectStorageClient client;

    @Inject
    AuthenticationDetailsProvider detailsProvider;

    public String handleRequest() {
        final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
        builder.namespaceName("kg");
        builder.compartmentId(detailsProvider.getTenantId());
        final List<BucketSummary> items = client
                .listBuckets(builder.build())
                .getItems();

        assertNotNull(items);
        return "ok";
    }
}
