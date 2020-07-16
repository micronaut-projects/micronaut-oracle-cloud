package example;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.oci.function.OciFunction;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ListBucketsFunction extends OciFunction {
    @Inject
    ObjectStorageClient objectStorageClient;

    @Inject
    AuthenticationDetailsProvider detailsProvider;

    @ReflectiveAccess
    public List<String> handleRequest() {
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(detailsProvider.getTenantId()).build();
        String namespace = objectStorageClient.getNamespace(getNamespaceRequest).getValue();
        final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
        builder.namespaceName(namespace);
        builder.compartmentId(detailsProvider.getTenantId());
        return objectStorageClient.listBuckets(builder.build())
                .getItems().stream().map(BucketSummary::getName)
                .collect(Collectors.toList());
    }
}
