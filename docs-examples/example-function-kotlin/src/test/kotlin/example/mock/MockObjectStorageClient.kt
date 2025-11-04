package example.mock

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.BucketSummary
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton

@Singleton
@Replaces(ObjectStorageClient::class)
class MockObjectStorageClient(authDetailsProvider: BasicAuthenticationDetailsProvider?) :
    ObjectStorageClient(authDetailsProvider) {

    override fun getNamespace(request: GetNamespaceRequest?): GetNamespaceResponse? {
        return GetNamespaceResponse.builder().value(MockData.namespace).build()
    }

    override fun listBuckets(request: ListBucketsRequest?): ListBucketsResponse? {
        val bucketSummaries = MockData.bucketNames.stream()
            .map { name: String? -> BucketSummary.builder().name(name).build() }
            .toList()
        return ListBucketsResponse.builder()
            .items(bucketSummaries)
            .build()
    }
}
