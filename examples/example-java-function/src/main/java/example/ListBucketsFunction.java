package example;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.oci.function.OciFunction;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ListBucketsFunction extends OciFunction {
    @Inject
    ObjectStorageClient objectStorageClient;

    @Inject
    TenantIdProvider tenantIdProvider;

    @ReflectiveAccess
    public List<String> handleRequest() {
        try {
            String tenancyId = tenantIdProvider.getTenantId();

            GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build();
            String namespace = objectStorageClient.getNamespace(getNamespaceRequest).getValue();
            final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
            builder.namespaceName(namespace);
            builder.compartmentId(tenancyId);
            return objectStorageClient.listBuckets(builder.build())
                    .getItems().stream().map(BucketSummary::getName)
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            System.out.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }

    }
}
