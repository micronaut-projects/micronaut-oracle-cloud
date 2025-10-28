package example.mock

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.BucketSummary
import com.oracle.bmc.objectstorage.model.ListObjects
import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse
import com.oracle.bmc.objectstorage.responses.DeleteBucketResponse
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton

@Singleton
@Replaces(ObjectStorageClient::class)
class MockObjectStorageClient(authDetailsProvider: BasicAuthenticationDetailsProvider?) :
    ObjectStorageClient(authDetailsProvider) {

    override fun getNamespace(request: GetNamespaceRequest?): GetNamespaceResponse? {
        return GetNamespaceResponse.builder().value(MockData.namespace).build()
    }

    override fun createBucket(request: CreateBucketRequest): CreateBucketResponse? {
        MockData.bucketNames.add(request.createBucketDetails.name)

        return CreateBucketResponse.builder()
            .location(MockData.bucketLocation)
            .build()
    }

    override fun listBuckets(request: ListBucketsRequest?): ListBucketsResponse? {
        val bucketSummaries = MockData.bucketNames.stream()
            .map { name: String? -> BucketSummary.builder().name(name).build() }
            .toList()
        return ListBucketsResponse.builder()
            .items(bucketSummaries)
            .build()
    }

    override fun listObjects(request: ListObjectsRequest?): ListObjectsResponse? {
        val objects = MockData.objectNames.stream()
            .map { name: String? -> ObjectSummary.builder().name(name).build() }
            .toList()
        return ListObjectsResponse.builder().listObjects(
            ListObjects.builder().objects(objects).build()
        ).build()
    }

    override fun deleteBucket(request: DeleteBucketRequest?): DeleteBucketResponse? {
        return DeleteBucketResponse.builder().build()
    }
}
