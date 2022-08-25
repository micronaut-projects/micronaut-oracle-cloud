package io.micronaut.oraclecloud.mock;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.responses.AsyncHandler;
import io.micronaut.context.annotation.Replaces;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Singleton
@Replaces(ObjectStorageAsyncClient.class)
public class MockObjectStorageAsyncClient extends ObjectStorageAsyncClient {

    public MockObjectStorageAsyncClient(BasicAuthenticationDetailsProvider authDetailsProvider) {
        super(authDetailsProvider);
    }

    @Override
    public Future<ListBucketsResponse> listBuckets(ListBucketsRequest request,
                                                   AsyncHandler<ListBucketsRequest, ListBucketsResponse> handler) {

        List<BucketSummary> bucketSummaries = MockData.bucketNames.stream()
                .map(name -> BucketSummary.builder().name(name).build())
                .collect(Collectors.toList());

        ListBucketsResponse response = ListBucketsResponse.builder().items(bucketSummaries).build();
        handler.onSuccess(request, response);
        return CompletableFuture.completedFuture(response);
    }
}
