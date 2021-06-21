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
import io.micronaut.oraclecloud.clients.rxjava2.objectstorage.ObjectStorageRxClient
import io.micronaut.oraclecloud.core.TenancyIdProvider
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.model.CreateBucketDetails
import com.oracle.bmc.objectstorage.responses.CreateBucketResponse
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.responses.DeleteBucketResponse
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.reactivex.Single
// end::imports[]

// tag::class[]
@Controller("/os")
class BucketController(private val objectStorage: ObjectStorageRxClient,
                       private val tenancyIdProvider: TenancyIdProvider) // <1>
    : BucketOperations {
// end::class[]

    @Get("/buckets{/compartmentId}")
    override fun listBuckets(@PathVariable @Nullable compartmentId: String?): Single<List<String>> {
        val compartmentOcid = compartmentId ?: tenancyIdProvider.tenancyId!!
        val getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(compartmentOcid).build()
        return objectStorage.getNamespace(getNamespaceRequest).flatMap { namespaceResponse: GetNamespaceResponse ->
            val listBucketsRequest = ListBucketsRequest.builder()
                    .namespaceName(namespaceResponse.value)
                    .compartmentId(compartmentOcid)
                    .build()
            objectStorage.listBuckets(listBucketsRequest)
                    .map { listBucketsResponse: ListBucketsResponse ->
                        listBucketsResponse.items.map { it.name }
                    }
        }
    }

    // tag::method[]
    @Post(value = "/buckets/{name}")
    override fun createBucket(name: String): Single<String> {
        val tenancyId = tenancyIdProvider.tenancyId
        val getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build()
        return objectStorage.getNamespace(getNamespaceRequest) // <1>
                .flatMap { namespaceResponse: GetNamespaceResponse ->
                    val createBucketRequest = CreateBucketRequest.builder()
                            .namespaceName(namespaceResponse.value)
                            .createBucketDetails(CreateBucketDetails.builder()
                                    .compartmentId(tenancyId)
                                    .name(name)
                                    .build())
                            .build()
                    objectStorage.createBucket(createBucketRequest) // <2>
                            .map { obj: CreateBucketResponse -> obj.location } // <3>
                }
    }
    // end::method[]

    @Delete(value = "/buckets/{name}")
    override fun deleteBucket(name: String): Single<Boolean> {
        val getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.tenancyId).build()
        return objectStorage.getNamespace(getNamespaceRequest).flatMap { getNamespaceResponse: GetNamespaceResponse ->
            val deleteBucketRequest = DeleteBucketRequest.builder()
                    .namespaceName(getNamespaceResponse.value)
                    .bucketName(name)
                    .build()
            objectStorage.deleteBucket(deleteBucketRequest)
                    .map { response: DeleteBucketResponse? -> true }
        }
    }
// tag::class[]
}
// end::class[]
