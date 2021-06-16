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
import com.oracle.bmc.objectstorage.model.CreateBucketDetails
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.oraclecloud.clients.rxjava2.objectstorage.ObjectStorageRxClient
import io.micronaut.oraclecloud.core.TenancyIdProvider
import io.reactivex.Single
// end::imports[]

// tag::class[]
@CompileStatic
@Controller('/os')
class BucketController implements BucketOperations {

    private final ObjectStorageRxClient objectStorage
    private final TenancyIdProvider tenancyIdProvider

    BucketController(ObjectStorageRxClient objectStorage,
                     TenancyIdProvider tenancyIdProvider) { // <1>
        this.objectStorage = objectStorage
        this.tenancyIdProvider = tenancyIdProvider
    }
// end::class[]

    @Override
    @Get('/buckets{/compartmentId}')
    Single<List<String>> listBuckets(@PathVariable @Nullable String compartmentId) {

        String compartmentOcid = compartmentId ?: tenancyIdProvider.tenancyId

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(compartmentOcid).build()

        return objectStorage.getNamespace(getNamespaceRequest).flatMap(namespaceResponse -> {
            ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                    .namespaceName(namespaceResponse.value)
                    .compartmentId(compartmentOcid)
                    .build()
            return objectStorage.listBuckets(listBucketsRequest)
                    .map(listBucketsResponse -> listBucketsResponse.items*.name)
        }) as Single<List<String>>
    }

    // tag::method[]
    @Override
    @Post(value = '/buckets/{name}')
    Single<String> createBucket(String name) {

        String tenancyId = tenancyIdProvider.tenancyId
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build()
        return objectStorage.getNamespace(getNamespaceRequest) // <1>
                .flatMap(namespaceResponse -> {
                    CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                            .namespaceName(namespaceResponse.value)
                            .createBucketDetails(CreateBucketDetails.builder()
                                    .compartmentId(tenancyId)
                                    .name(name)
                                    .build())
                            .build()

                    return objectStorage.createBucket(createBucketRequest) // <2>
                            .map(CreateBucketResponse::getLocation) // <3>
                }) as Single<String>
    }
    // end::method[]

    @Override
    @Delete(value = '/buckets/{name}')
    Single<Boolean> deleteBucket(String name) {
        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.tenancyId).build()
        return objectStorage.getNamespace(getNamespaceRequest).flatMap(getNamespaceResponse -> {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                    .namespaceName(getNamespaceResponse.value)
                    .bucketName(name)
                    .build()

            return objectStorage.deleteBucket(deleteBucketRequest)
                    .map(response -> true)
        })
    }
// tag::class[]
}
// end::class[]
