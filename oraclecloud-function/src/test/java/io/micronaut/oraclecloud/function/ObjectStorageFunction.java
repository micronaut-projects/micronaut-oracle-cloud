package io.micronaut.oraclecloud.function;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import io.micronaut.oraclecloud.core.TenancyIdProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ObjectStorageFunction extends OciFunction {

    @Inject
    ObjectStorageClient client;

    @Inject
    TenancyIdProvider tenancyIdProvider;

    public String handleRequest() {
        ListBucketsRequest request = ListBucketsRequest.builder()
                .namespaceName("kg")
                .compartmentId(tenancyIdProvider.getTenancyId())
                .build();
        final List<BucketSummary> items = client
                .listBuckets(request)
                .getItems();

        return items.stream()
                .map(BucketSummary::getName)
                .collect(Collectors.toList())
                .toString();
    }
}
