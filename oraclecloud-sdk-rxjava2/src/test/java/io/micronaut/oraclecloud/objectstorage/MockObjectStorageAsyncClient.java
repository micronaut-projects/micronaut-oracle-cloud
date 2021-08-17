package io.micronaut.oraclecloud.objectstorage;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.responses.AsyncHandler;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Singleton
@Replaces(ObjectStorageAsyncClient.class)
class MockObjectStorageAsyncClient extends ObjectStorageAsyncClient {

    MockObjectStorageAsyncClient(BasicAuthenticationDetailsProvider authenticationDetailsProvider) {
        super(authenticationDetailsProvider);
    }

    @Override
    public Future<ListBucketsResponse> listBuckets(ListBucketsRequest request,
                                                   AsyncHandler<ListBucketsRequest, ListBucketsResponse> handler) {
        final ListBucketsResponse response = ListBucketsResponse.builder()
                .items(Arrays.asList(
                        BucketSummary.builder().name("b1").build(),
                        BucketSummary.builder().name("b2").build()
                ))
                .build();
        handler.onSuccess(request, response);
        return CompletableFuture.completedFuture(response);
    }
}

