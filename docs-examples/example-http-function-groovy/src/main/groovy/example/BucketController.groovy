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
package example

// tag::imports[]
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.model.CreateBucketDetails
import com.oracle.bmc.objectstorage.model.ListObjects
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.oraclecloud.core.TenancyIdProvider
// end::imports[]

// tag::class[]
@CompileStatic
@Controller('/os')
class BucketController {

    private final ObjectStorage objectStorage
    private final TenancyIdProvider tenancyIdProvider

    BucketController(ObjectStorage objectStorage,
                     TenancyIdProvider tenancyIdProvider) { // <1>
        this.objectStorage = objectStorage
        this.tenancyIdProvider = tenancyIdProvider
    }
// end::class[]

    // tag::listBuckets[]
    @Get('/buckets{/compartmentId}')
    List<String> listBuckets(@PathVariable @Nullable String compartmentId) {

        String compartmentOcid = compartmentId ?: tenancyIdProvider.tenancyId

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(compartmentOcid).build()
        String namespace = objectStorage.getNamespace(getNamespaceRequest).value

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName(namespace)
                .compartmentId(compartmentOcid)
                .build()

        return objectStorage.listBuckets(listBucketsRequest).items*.name
    }
    // end::listBuckets[]

    @Post(uri = '/echo/{name}', produces = MediaType.TEXT_PLAIN)
    String testPost(@PathVariable String name) {
        println '/echo as POST'
        return 'Hello, ' + name
    }

    @Get(uri = '/echo/{name}', produces = MediaType.TEXT_PLAIN)
    String testGet(@PathVariable String name) {
        println '/echo as GET'
        return 'Hello, ' + name
    }

    @Get('/objects/{bucketName}{/start}')
    Map<String, Object> listObjects(@PathVariable String bucketName,
                                    @PathVariable @Nullable String start,
                                    @QueryValue Optional<Integer> limit) {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.tenancyId).build()
        String namespace = objectStorage.getNamespace(getNamespaceRequest).value

        ListObjectsRequest.Builder listObjectsRequestBuilder = ListObjectsRequest.builder()
                .bucketName(bucketName)
                .limit(limit.orElse(25))
                .namespaceName(namespace)
        if (start) {
            listObjectsRequestBuilder.start(start)
        }
        ListObjects listObjects = objectStorage.listObjects(listObjectsRequestBuilder.build())
                .listObjects
        String next = listObjects.nextStartWith
        List<String> objects = listObjects.objects*.name

        return [nextStart: next ?: '',
                objects: objects] as Map<String, Object>
    }

    // tag::method[]
    @Post(value = '/buckets/{name}')
    String createBucket(@PathVariable String name) {

        String tenancyId = tenancyIdProvider.tenancyId

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build()
        String namespace = objectStorage.getNamespace(getNamespaceRequest).value // <1>

        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .namespaceName(namespace)
                .createBucketDetails(CreateBucketDetails.builder()
                        .compartmentId(tenancyId)
                        .name(name)
                        .build())
                .build()

        return objectStorage.createBucket(createBucketRequest) // <2>
                .location // <3>
    }
    // end::method[]

    @Delete(value = '/buckets/{name}')
    boolean deleteBucket(@PathVariable String name) {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.tenancyId).build()
        String namespace = objectStorage.getNamespace(getNamespaceRequest).value

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .namespaceName(namespace)
                .bucketName(name)
                .build()

        objectStorage.deleteBucket(deleteBucketRequest)
        return true
    }
// tag::class[]
}
// end::class[]
