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
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest;
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.oraclecloud.core.TenancyIdProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
// end::imports[]

// tag::class[]
@Controller("/os")
public class BucketController {

    private final ObjectStorage objectStorage;
    private final TenancyIdProvider tenancyIdProvider;

    public BucketController(ObjectStorage objectStorage,
                            TenancyIdProvider tenancyIdProvider) { // <1>
        this.objectStorage = objectStorage;
        this.tenancyIdProvider = tenancyIdProvider;
    }
// end::class[]

    // tag::listBuckets[]
    @Get("/buckets{/compartmentId}")
    public List<String> listBuckets(@PathVariable @Nullable String compartmentId) {

        String compartmentOcid = compartmentId != null ? compartmentId : tenancyIdProvider.getTenancyId();

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(compartmentOcid).build();
        String namespace = objectStorage.getNamespace(getNamespaceRequest).getValue();

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName(namespace)
                .compartmentId(compartmentOcid)
                .build();

        return objectStorage.listBuckets(listBucketsRequest)
                .getItems()
                .stream()
                .map(BucketSummary::getName)
                .collect(Collectors.toList());
    }
    // end::listBuckets[]

    @Post(uri = "/echo/{name}", produces = MediaType.TEXT_PLAIN)
    public String testPost(@PathVariable String name) {
        System.out.println("/echo as POST");
        return "Hello, " + name;
    }

    @Get(uri = "/echo/{name}", produces = MediaType.TEXT_PLAIN)
    public String testGet(@PathVariable String name) {
        System.out.println("/echo as GET");
        return "Hello, " + name;
    }

    @Get("/objects/{bucketName}{/start}")
    public Map<String, Object> listObjects(@PathVariable String bucketName,
                                           @PathVariable @Nullable String start,
                                           @QueryValue Optional<Integer> limit) {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.getTenancyId()).build();
        String namespace = objectStorage.getNamespace(getNamespaceRequest).getValue();

        ListObjectsRequest.Builder listObjectsRequestBuilder = ListObjectsRequest.builder()
                .bucketName(bucketName)
                .limit(limit.orElse(25))
                .namespaceName(namespace);
        if (start != null) {
            listObjectsRequestBuilder.start(start);
        }
        ListObjects listObjects = objectStorage.listObjects(listObjectsRequestBuilder.build())
                .getListObjects();
        String next = listObjects.getNextStartWith();
        List<String> objects = listObjects
                .getObjects()
                .stream()
                .map(ObjectSummary::getName)
                .collect(Collectors.toList());
        return CollectionUtils.mapOf(
                "nextStart", next != null ? next : "",
                "objects", objects
        );
    }

    // tag::method[]
    @Post(value = "/buckets/{name}")
    public String createBucket(@PathVariable String name) {

        String tenancyId = tenancyIdProvider.getTenancyId();

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build();
        String namespace = objectStorage.getNamespace(getNamespaceRequest).getValue(); // <1>

        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .namespaceName(namespace)
                .createBucketDetails(CreateBucketDetails.builder()
                        .compartmentId(tenancyId)
                        .name(name)
                        .build())
                .build();

        return objectStorage.createBucket(createBucketRequest) // <2>
                .getLocation(); // <3>
    }
    // end::method[]

    @Delete(value = "/buckets/{name}")
    public boolean deleteBucket(@PathVariable String name) {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.getTenancyId()).build();
        String namespace = objectStorage.getNamespace(getNamespaceRequest).getValue();

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .namespaceName(namespace)
                .bucketName(name)
                .build();

        objectStorage.deleteBucket(deleteBucketRequest);
        return true;
    }
// tag::class[]
}
// end::class[]
