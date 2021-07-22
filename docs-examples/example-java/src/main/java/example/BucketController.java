/*
 * Copyright 2017-2021 original authors
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
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.oraclecloud.clients.reactor.objectstorage.ObjectStorageReactorClient;
import io.micronaut.oraclecloud.core.TenancyIdProvider;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
// end::imports[]

// tag::class[]
@Controller("/os")
public class BucketController implements BucketOperations {

    private final ObjectStorageReactorClient objectStorage;
    private final TenancyIdProvider tenancyIdProvider;

    public BucketController(ObjectStorageReactorClient objectStorage,
                            TenancyIdProvider tenancyIdProvider) { // <1>
        this.objectStorage = objectStorage;
        this.tenancyIdProvider = tenancyIdProvider;
    }
// end::class[]

    @Override
    @Get("/buckets{/compartmentId}")
    public Mono<List<String>> listBuckets(@PathVariable @Nullable String compartmentId) {

        String compartmentOcid = compartmentId != null ? compartmentId : tenancyIdProvider.getTenancyId();

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(compartmentOcid).build();

        return objectStorage.getNamespace(getNamespaceRequest).flatMap(namespaceResponse -> {
            ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                    .namespaceName(namespaceResponse.getValue())
                    .compartmentId(compartmentOcid)
                    .build();
            return objectStorage.listBuckets(listBucketsRequest)
                    .map(listBucketsResponse -> listBucketsResponse.getItems()
                            .stream()
                            .map(BucketSummary::getName)
                            .collect(Collectors.toList()));
        });
    }

    // tag::method[]
    @Override
    @Post(value = "/buckets/{name}")
    public Mono<String> createBucket(String name) {

        String tenancyId = tenancyIdProvider.getTenancyId();
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build();
        return objectStorage.getNamespace(getNamespaceRequest) // <1>
                .flatMap(namespaceResponse -> {
                    CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                            .namespaceName(namespaceResponse.getValue())
                            .createBucketDetails(CreateBucketDetails.builder()
                                    .compartmentId(tenancyId)
                                    .name(name)
                                    .build())
                            .build();

                    return objectStorage.createBucket(createBucketRequest) // <2>
                            .map(CreateBucketResponse::getLocation); // <3>
                });
    }
    // end::method[]

    @Override
    @Delete(value = "/buckets/{name}")
    public Mono<Boolean> deleteBucket(String name) {
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.getTenancyId()).build();
        return objectStorage.getNamespace(getNamespaceRequest).flatMap(getNamespaceResponse -> {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                    .namespaceName(getNamespaceResponse.getValue())
                    .bucketName(name)
                    .build();

            return objectStorage.deleteBucket(deleteBucketRequest)
                    .map(response -> true);
        });
    }
// tag::class[]
}
// end::class[]
