package example.mock;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse;
import com.oracle.bmc.objectstorage.responses.DeleteBucketResponse;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import io.micronaut.context.annotation.Replaces;

import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Replaces(ObjectStorageClient.class)
public class MockObjectStorageClient extends ObjectStorageClient {

    public MockObjectStorageClient(BasicAuthenticationDetailsProvider authDetailsProvider) {
        super(authDetailsProvider);
    }

    @Override
    public GetNamespaceResponse getNamespace(GetNamespaceRequest request) {
        return GetNamespaceResponse.builder().value(MockData.namespace).build();
    }

    @Override
    public CreateBucketResponse createBucket(CreateBucketRequest request) {
        MockData.bucketNames.add(request.getCreateBucketDetails().getName());

        return CreateBucketResponse.builder()
                .location(MockData.bucketLocation)
                .build();
    }

    @Override
    public ListBucketsResponse listBuckets(ListBucketsRequest request) {
        List<BucketSummary> bucketSummaries = MockData.bucketNames.stream()
                .map(name -> BucketSummary.builder().name(name).build())
                .collect(Collectors.toList());
        return ListBucketsResponse.builder()
                .items(bucketSummaries)
                .build();
    }

    @Override
    public ListObjectsResponse listObjects(ListObjectsRequest request) {
        List<ObjectSummary> objects = MockData.objectNames.stream()
                .map(name -> ObjectSummary.builder().name(name).build())
                .collect(Collectors.toList());
        return ListObjectsResponse.builder().listObjects(
                ListObjects.builder().objects(objects).build()
        ).build();
    }

    @Override
    public DeleteBucketResponse deleteBucket(DeleteBucketRequest request) {
        return DeleteBucketResponse.builder().build();
    }
}
