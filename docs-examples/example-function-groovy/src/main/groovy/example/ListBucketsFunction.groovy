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
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.ReflectiveAccess
import io.micronaut.oraclecloud.core.TenancyIdProvider
import io.micronaut.oraclecloud.function.OciFunction

import jakarta.inject.Inject
import jakarta.inject.Singleton
// end::imports[]

// tag::class[]
@CompileStatic
@Singleton
class ListBucketsFunction extends OciFunction { // <1>

    @Inject
    ObjectStorageClient objectStorageClient // <2>

    @Inject
    TenancyIdProvider tenantIdProvider

// end::class[]

    // tag::method[]
    @ReflectiveAccess
    List<String> handleRequest() {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenantIdProvider.tenancyId).build()
        String namespace = objectStorageClient.getNamespace(getNamespaceRequest).value

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName(namespace)
                .compartmentId(tenantIdProvider.tenancyId)
                .build()

        return objectStorageClient.listBuckets(listBucketsRequest).items*.name
    }
    // end::method[]

// tag::class[]
}
// end::class[]
