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
import com.oracle.bmc.objectstorage.requests.CreateBucketRequest
import com.oracle.bmc.objectstorage.requests.DeleteBucketRequest
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.oraclecloud.core.TenancyIdProvider
import java.util.Optional
// end::imports[]

// tag::class[]
@Controller("/os")
class BucketController(private val objectStorage: ObjectStorage,
                       private val tenancyIdProvider: TenancyIdProvider) { // <1>
// end::class[]

    // tag::listBuckets[]
    @Get("/buckets{/compartmentId}")
    fun listBuckets(@PathVariable @Nullable compartmentId: String?): List<String> {
        val compartmentOcid = compartmentId ?: tenancyIdProvider.tenancyId!!
        val getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(compartmentOcid).build()
        val namespace = objectStorage.getNamespace(getNamespaceRequest).value
        val listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName(namespace)
                .compartmentId(compartmentOcid)
                .build()
        return objectStorage.listBuckets(listBucketsRequest).items
                .map { it.name }
    }
    // end::listBuckets[]

    @Post(uri = "/echo/{name}", produces = [MediaType.TEXT_PLAIN])
    fun testPost(@PathVariable name: String): String {
        println("/echo as POST")
        return "Hello, $name"
    }

    @Get(uri = "/echo/{name}", produces = [MediaType.TEXT_PLAIN])
    fun testGet(@PathVariable name: String): String {
        println("/echo as GET")
        return "Hello, $name"
    }

    @Get("/objects/{bucketName}{/start}")
    fun listObjects(@PathVariable bucketName: String,
                    @PathVariable @Nullable start: String?,
                    @QueryValue limit: Optional<Int?>): Map<String, Any> {
        val getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.tenancyId).build()
        val namespace = objectStorage.getNamespace(getNamespaceRequest).value
        val listObjectsRequestBuilder = ListObjectsRequest.builder()
                .bucketName(bucketName)
                .limit(limit.orElse(25))
                .namespaceName(namespace)
        if (start != null) {
            listObjectsRequestBuilder.start(start)
        }
        val listObjects = objectStorage.listObjects(listObjectsRequestBuilder.build())
                .listObjects
        val next = listObjects.nextStartWith
        val objects = listObjects.objects.map { it.name }
        return mapOf("nextStart" to (next ?: ""), "objects" to objects)
    }

    // tag::method[]
    @Post(value = "/buckets/{name}")
    fun createBucket(@PathVariable name: String?): String {
        val tenancyId = tenancyIdProvider.tenancyId
        val getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyId).build()
        val namespace = objectStorage.getNamespace(getNamespaceRequest).value // <1>
        val createBucketRequest = CreateBucketRequest.builder()
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

    @Delete(value = "/buckets/{name}")
    fun deleteBucket(@PathVariable name: String?): Boolean {
        val getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenancyIdProvider.tenancyId).build()
        val namespace = objectStorage.getNamespace(getNamespaceRequest).value
        val deleteBucketRequest = DeleteBucketRequest.builder()
                .namespaceName(namespace)
                .bucketName(name)
                .build()
        objectStorage.deleteBucket(deleteBucketRequest)
        return true
    }
// tag::class[]
}
// end::class[]
