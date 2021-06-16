package example.mock

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.model.BucketSummary
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse
import com.oracle.bmc.objectstorage.responses.DeleteBucketResponse
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import com.oracle.bmc.responses.AsyncHandler
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces

import javax.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

@CompileStatic
@Singleton
@Replaces(ObjectStorageAsyncClient)
class MockObjectStorageAsyncClient extends ObjectStorageAsyncClient {

    MockObjectStorageAsyncClient(BasicAuthenticationDetailsProvider authDetailsProvider) {
        super(authDetailsProvider)
    }

    @Override
    Future<GetNamespaceResponse> getNamespace(GetNamespaceRequest request,
                                              AsyncHandler<GetNamespaceRequest, GetNamespaceResponse> handler) {
        GetNamespaceResponse response = GetNamespaceResponse.builder().value(MockData.namespace).build()
        handler.onSuccess request, response
        return CompletableFuture.completedFuture(response)
    }

    @Override
    Future<CreateBucketResponse> createBucket(CreateBucketRequest request,
                                              AsyncHandler<CreateBucketRequest, CreateBucketResponse> handler) {

        MockData.bucketNames << request.createBucketDetails.name

        CreateBucketResponse response = CreateBucketResponse.builder().location(MockData.bucketLocation).build()
        handler.onSuccess request, response
        return CompletableFuture.completedFuture(response)
    }

    @Override
    Future<ListBucketsResponse> listBuckets(ListBucketsRequest request,
                                            AsyncHandler<ListBucketsRequest, ListBucketsResponse> handler) {

        List<BucketSummary> bucketSummaries = MockData.bucketNames
                .collect { BucketSummary.builder().name(it).build() }

        ListBucketsResponse response = ListBucketsResponse.builder().items(bucketSummaries).build()
        handler.onSuccess request, response
        return CompletableFuture.completedFuture(response)
    }

    @Override
    Future<DeleteBucketResponse> deleteBucket(DeleteBucketRequest request,
                                              AsyncHandler<DeleteBucketRequest, DeleteBucketResponse> handler) {

        MockData.bucketNames.remove request.bucketName

        DeleteBucketResponse response = DeleteBucketResponse.builder().build()
        handler.onSuccess request, response
        return CompletableFuture.completedFuture(response)
    }
}
