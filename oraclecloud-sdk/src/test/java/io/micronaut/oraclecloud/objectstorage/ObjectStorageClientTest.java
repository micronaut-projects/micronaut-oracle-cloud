package io.micronaut.oraclecloud.objectstorage;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.responses.AsyncHandler;
import io.micronaut.oraclecloud.clients.rxjava2.objectstorage.ObjectStorageRxClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class ObjectStorageClientTest {

    private final ObjectStorageClient objectStorageClient;
    private final AuthenticationDetailsProvider detailsProvider;
    private final ObjectStorageAsync objectStorageAsync;
    private final ObjectStorageRxClient objectStorageRxClient;

    public ObjectStorageClientTest(
            ObjectStorageClient objectStorageClient,
            ObjectStorageAsync objectStorageAsync,
            AuthenticationDetailsProvider detailsProvider,
            ObjectStorageRxClient objectStorageRxClient) {
        this.objectStorageClient = objectStorageClient;
        this.detailsProvider = detailsProvider;
        this.objectStorageAsync = objectStorageAsync;
        this.objectStorageRxClient = objectStorageRxClient;
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

    @Test
    void testObjectStorageClientRx() {
        final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
        builder.namespaceName("kg");
        builder.compartmentId(detailsProvider.getTenantId());
        final List<BucketSummary> items = objectStorageRxClient
                .listBuckets(builder.build())
                .blockingGet()
                .getItems();

        assertNotNull(items);
    }

    @Test
    void testObjectStorageAsync() throws ExecutionException, InterruptedException {
        final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
        builder.namespaceName("kg");
        builder.compartmentId(detailsProvider.getTenantId());
        CompletableFuture<List<BucketSummary>> future = new CompletableFuture<>();
        objectStorageAsync
                .listBuckets(builder.build(), new AsyncHandler<ListBucketsRequest, ListBucketsResponse>() {
                    @Override
                    public void onSuccess(ListBucketsRequest listBucketsRequest, ListBucketsResponse listBucketsResponse) {
                        future.complete(listBucketsResponse.getItems());
                    }

                    @Override
                    public void onError(ListBucketsRequest listBucketsRequest, Throwable error) {
                        future.completeExceptionally(error);
                    }
                });

        assertNotNull(future.get());
    }
}
