package io.micronaut.oraclecloud.objectstorage;

import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.responses.AsyncHandler;
import io.micronaut.oraclecloud.clients.rxjava2.objectstorage.ObjectStorageRxClient;
import io.micronaut.oraclecloud.core.TenancyIdProvider;
import io.micronaut.oraclecloud.mock.MockData;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class ObjectStorageClientTest {

    private final ObjectStorageClient objectStorageClient;
    private final TenancyIdProvider tenancyIdProvider;
    private final ObjectStorageAsync objectStorageAsync;
    private final ObjectStorageRxClient objectStorageRxClient;

    public ObjectStorageClientTest(
            ObjectStorageClient objectStorageClient,
            ObjectStorageAsync objectStorageAsync,
            TenancyIdProvider tenancyIdProvider,
            ObjectStorageRxClient objectStorageRxClient) {
        this.objectStorageClient = objectStorageClient;
        this.tenancyIdProvider = tenancyIdProvider;
        this.objectStorageAsync = objectStorageAsync;
        this.objectStorageRxClient = objectStorageRxClient;
    }

    @Test
    void testObjectStorageClient() {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName("kg")
                .compartmentId(tenancyIdProvider.getTenancyId())
                .build();
        final List<BucketSummary> items = objectStorageClient
                .listBuckets(listBucketsRequest)
                .getItems();

        List<String> names = items.stream().map(BucketSummary::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("b1", "b2"), names);
    }

    @Test
    void testObjectStorageClientRx() {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName("kg")
                .compartmentId(tenancyIdProvider.getTenancyId())
                .build();
        final List<BucketSummary> items = objectStorageRxClient
                .listBuckets(listBucketsRequest)
                .blockingGet()
                .getItems();

        List<String> names = items.stream().map(BucketSummary::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("b1", "b2"), names);
    }

    @Test
    void testObjectStorageAsync() throws ExecutionException, InterruptedException {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName("kg")
                .compartmentId(tenancyIdProvider.getTenancyId())
                .build();
        CompletableFuture<List<BucketSummary>> future = new CompletableFuture<>();
        objectStorageAsync
                .listBuckets(listBucketsRequest, new AsyncHandler<ListBucketsRequest, ListBucketsResponse>() {
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

    @BeforeEach
    void setup() {
        MockData.bucketNames.add("b1");
        MockData.bucketNames.add("b2");
    }

    @AfterEach
    void cleanup() {
        MockData.reset();
    }
}
