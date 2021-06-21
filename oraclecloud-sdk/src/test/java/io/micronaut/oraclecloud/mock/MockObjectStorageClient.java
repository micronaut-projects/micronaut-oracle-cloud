package io.micronaut.oraclecloud.mock;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import io.micronaut.context.annotation.Replaces;

import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Replaces(ObjectStorageClient.class)
public class MockObjectStorageClient extends ObjectStorageClient {

    public MockObjectStorageClient(BasicAuthenticationDetailsProvider authDetailsProvider) {
        super(authDetailsProvider);
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
}
