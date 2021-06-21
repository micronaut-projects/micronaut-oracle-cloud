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
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces

import javax.inject.Singleton

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
    public CreateBucketResponse createBucket(CreateBucketRequest request) {
        MockData.bucketNames << request.createBucketDetails.name

        return CreateBucketResponse.builder()
                .location(MockData.bucketLocation)
                .build()
    }

    @Override
    ListBucketsResponse listBuckets(ListBucketsRequest request) {
        List<BucketSummary> bucketSummaries = MockData.bucketNames
                .collect { BucketSummary.builder().name(it).build() }
        return ListBucketsResponse.builder()
                .items(bucketSummaries)
                .build()
    }

    @Override
    ListObjectsResponse listObjects(ListObjectsRequest request) {
        List<ObjectSummary> objects = MockData.objectNames
                .collect { ObjectSummary.builder().name(it).build() }
        return ListObjectsResponse.builder().listObjects(
                ListObjects.builder().objects(objects).build()
        ).build()
    }

    @Override
    DeleteBucketResponse deleteBucket(DeleteBucketRequest request) {
        return DeleteBucketResponse.builder().build()
    }
}
