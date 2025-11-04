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
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.stream.Collectors

@Singleton
@Replaces(ObjectStorageAsyncClient::class)
class MockObjectStorageAsyncClient(authDetailsProvider: BasicAuthenticationDetailsProvider?) :
    ObjectStorageAsyncClient(authDetailsProvider) {

    override fun getNamespace(
        request: GetNamespaceRequest?,
        handler: AsyncHandler<GetNamespaceRequest?, GetNamespaceResponse?>
    ): Future<GetNamespaceResponse?> {
        val response = GetNamespaceResponse.builder().value(MockData.namespace).build()
        handler.onSuccess(request, response)
        return CompletableFuture.completedFuture<GetNamespaceResponse?>(response)
    }

    override fun createBucket(
        request: CreateBucketRequest,
        handler: AsyncHandler<CreateBucketRequest?, CreateBucketResponse?>
    ): Future<CreateBucketResponse?> {
        MockData.bucketNames.add(request.createBucketDetails.name)

        val response = CreateBucketResponse.builder().location(MockData.bucketLocation).build()
        handler.onSuccess(request, response)
        return CompletableFuture.completedFuture<CreateBucketResponse?>(response)
    }

    override fun listBuckets(
        request: ListBucketsRequest?,
        handler: AsyncHandler<ListBucketsRequest?, ListBucketsResponse?>
    ): Future<ListBucketsResponse?> {
        val bucketSummaries = MockData.bucketNames.stream()
            .map { name: String? -> BucketSummary.builder().name(name).build() }
            .collect(Collectors.toList())

        val response = ListBucketsResponse.builder().items(bucketSummaries).build()
        handler.onSuccess(request, response)
        return CompletableFuture.completedFuture<ListBucketsResponse?>(response)
    }

    override fun deleteBucket(
        request: DeleteBucketRequest,
        handler: AsyncHandler<DeleteBucketRequest?, DeleteBucketResponse?>
    ): Future<DeleteBucketResponse?> {
        MockData.bucketNames.remove(request.bucketName)

        val response = DeleteBucketResponse.builder().build()
        handler.onSuccess(request, response)
        return CompletableFuture.completedFuture<DeleteBucketResponse?>(response)
    }
}
