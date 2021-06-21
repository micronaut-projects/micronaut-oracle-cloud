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
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.oraclecloud.core.TenancyIdProvider;
import io.micronaut.oraclecloud.function.OciFunction;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
// end::imports[]

// tag::class[]
@Singleton
public class ListBucketsFunction extends OciFunction { // <1>

    @Inject
    ObjectStorageClient objectStorageClient; // <2>

    @Inject
    TenancyIdProvider tenantIdProvider;
// end::class[]

    // tag::method[]
    @ReflectiveAccess
    public List<String> handleRequest() {

        GetNamespaceRequest getNamespaceRequest = GetNamespaceRequest.builder()
                .compartmentId(tenantIdProvider.getTenancyId()).build();
        String namespace = objectStorageClient.getNamespace(getNamespaceRequest).getValue();

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName(namespace)
                .compartmentId(tenantIdProvider.getTenancyId())
                .build();

        return objectStorageClient.listBuckets(listBucketsRequest)
                .getItems().stream().map(BucketSummary::getName)
                .collect(Collectors.toList());
    }
    // end::method[]

// tag::class[]
}
// end::class[]
