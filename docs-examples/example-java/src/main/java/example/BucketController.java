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

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.oci.clients.rxjava2.objectstorage.ObjectStorageRxClient;
import io.reactivex.Single;

import java.util.List;
import java.util.stream.Collectors;

@Controller("/os")
public class BucketController implements BucketOperations {
    private final ObjectStorageRxClient objectStorage;
    private final AuthenticationDetailsProvider detailsProvider;

    public BucketController(ObjectStorageRxClient objectStorage, AuthenticationDetailsProvider detailsProvider) {
        this.objectStorage = objectStorage;
        this.detailsProvider = detailsProvider;

    }

    @Override
    @Get("/buckets")
    public Single<List<String>> listBuckets() {
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(detailsProvider.getTenantId()).build();
        return objectStorage.getNamespace(getNamespaceRequest).flatMap(getNamespaceResponse -> {
            final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
            builder.namespaceName(getNamespaceResponse.getValue());
            builder.compartmentId(detailsProvider.getTenantId());
            return objectStorage.listBuckets(builder.build())
                    .map(listBucketsResponse -> listBucketsResponse.getItems()
                            .stream()
                            .map(BucketSummary::getName)
                            .collect(Collectors.toList()));
        });

    }

    @Override
    @Post(value = "/buckets/{name}")
    public Single<String> createBucket(String name) {
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(detailsProvider.getTenantId()).build();
        return objectStorage.getNamespace(getNamespaceRequest).flatMap(getNamespaceResponse -> {
            CreateBucketRequest.Builder builder = CreateBucketRequest.builder()
                    .namespaceName(getNamespaceResponse.getValue())
                    .createBucketDetails(CreateBucketDetails.builder()
                            .compartmentId(detailsProvider.getTenantId())
                            .name(name)
                            .build());

            return objectStorage.createBucket(builder.build())
                    .map(CreateBucketResponse::getLocation);
        });

    }

    @Override
    @Delete(value = "/buckets/{name}")
    public Single<Boolean> deleteBucket(String name) {
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(detailsProvider.getTenantId()).build();
        return objectStorage.getNamespace(getNamespaceRequest).flatMap(getNamespaceResponse -> {
            DeleteBucketRequest.Builder builder = DeleteBucketRequest.builder()
                    .namespaceName(getNamespaceResponse.getValue())
                    .bucketName(name);

            return objectStorage.deleteBucket(builder.build())
                    .map(response -> Boolean.TRUE);
        });

    }
}
