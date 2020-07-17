/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example;

// tag::imports[]
import com.oracle.bmc.objectstorage.model.*;
import com.oracle.bmc.objectstorage.requests.*;
import com.oracle.bmc.objectstorage.responses.*;
import io.micronaut.http.annotation.*;
import io.micronaut.oci.clients.rxjava2.objectstorage.ObjectStorageRxClient;
import io.micronaut.oci.core.TenancyIdProvider;
import io.reactivex.Single;
import java.util.List;
import java.util.stream.Collectors;
// end::imports[]

// tag::class[]
@Controller("/os")
public class BucketController implements BucketOperations {
    private final ObjectStorageRxClient objectStorage;
    private final TenancyIdProvider tenancyIdProvider;

    public BucketController(
            ObjectStorageRxClient objectStorage,
            TenancyIdProvider tenancyIdProvider) { // <1>
        this.objectStorage = objectStorage;
        this.tenancyIdProvider = tenancyIdProvider;
    }
// end::class[]

    @Override
    @Get("/buckets")
    public Single<List<String>> listBuckets() {
        String tenancyId = tenancyIdProvider.getTenancyId();
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build();
        return objectStorage.getNamespace(getNamespaceRequest).flatMap(namespaceResponse -> {
            final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
            builder.namespaceName(namespaceResponse.getValue());
            builder.compartmentId(tenancyId);
            return objectStorage.listBuckets(builder.build())
                    .map(listBucketsResponse -> listBucketsResponse.getItems()
                            .stream()
                            .map(BucketSummary::getName)
                            .collect(Collectors.toList()));
        });

    }

    // tag::method[]
    @Override
    @Post(value = "/buckets/{name}")
    public Single<String> createBucket(String name) {
        String tenancyId = tenancyIdProvider.getTenancyId();
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build();
        return objectStorage.getNamespace(getNamespaceRequest) // <1>
                .flatMap(namespaceResponse -> {
            CreateBucketRequest.Builder builder = CreateBucketRequest.builder()
                    .namespaceName(namespaceResponse.getValue())
                    .createBucketDetails(CreateBucketDetails.builder()
                            .compartmentId(tenancyId)
                            .name(name)
                            .build());

            return objectStorage.createBucket(builder.build()) // <2>
                    .map(CreateBucketResponse::getLocation); // <3>
        });
    }
    // end::method[]

    @Override
    @Delete(value = "/buckets/{name}")
    public Single<Boolean> deleteBucket(String name) {
        String tenancyId = tenancyIdProvider.getTenancyId();
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build();
        return objectStorage.getNamespace(getNamespaceRequest).flatMap(getNamespaceResponse -> {
            DeleteBucketRequest.Builder builder = DeleteBucketRequest.builder()
                    .namespaceName(getNamespaceResponse.getValue())
                    .bucketName(name);

            return objectStorage.deleteBucket(builder.build())
                    .map(response -> Boolean.TRUE);
        });

    }
// tag::class[]
}
// end::class[]
