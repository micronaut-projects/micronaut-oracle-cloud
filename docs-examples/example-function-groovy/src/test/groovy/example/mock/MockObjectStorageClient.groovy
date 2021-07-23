package example.mock

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.BucketSummary
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces

import jakarta.inject.Singleton

@CompileStatic
@Singleton
@Replaces(ObjectStorageClient)
class MockObjectStorageClient extends ObjectStorageClient {

    MockObjectStorageClient(BasicAuthenticationDetailsProvider authDetailsProvider) {
        super(authDetailsProvider)
    }

    @Override
    GetNamespaceResponse getNamespace(GetNamespaceRequest request) {
        return GetNamespaceResponse.builder().value(MockData.namespace).build()
    }

    @Override
    ListBucketsResponse listBuckets(ListBucketsRequest request) {
        List<BucketSummary> bucketSummaries = MockData.bucketNames
                .collect { BucketSummary.builder().name(it).build() }
        return ListBucketsResponse.builder()
                .items(bucketSummaries)
                .build()
    }
}
