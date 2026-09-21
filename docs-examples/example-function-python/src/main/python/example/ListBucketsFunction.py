# tag::imports[]
from typing import Annotated

from com.oracle.bmc.objectstorage import ObjectStorageClient
from com.oracle.bmc.objectstorage.requests import GetNamespaceRequest, ListBucketsRequest
from jakarta.inject import Inject, Singleton
from micronaut.core.annotation import ReflectiveAccess
from micronaut.oraclecloud.core import TenancyIdProvider
from micronaut.oraclecloud.function import OciFunction
# end::imports[]


# tag::class[]
@Singleton
class ListBucketsFunction(OciFunction):  # <1>

    object_storage_client: Annotated[ObjectStorageClient, Inject]  # <2>

    tenant_id_provider: Annotated[TenancyIdProvider, Inject]
# end::class[]

    # tag::method[]
    @ReflectiveAccess
    def handle_request(self) -> list[str]:

        get_namespace_request = (GetNamespaceRequest.builder()
                                 .compartmentId(self.tenant_id_provider.getTenancyId()).build())
        namespace = self.object_storage_client.getNamespace(get_namespace_request).getValue()

        list_buckets_request = (ListBucketsRequest.builder()
                                .namespaceName(namespace)
                                .compartmentId(self.tenant_id_provider.getTenancyId())
                                .build())

        return [bucket.getName() for bucket in
                self.object_storage_client.listBuckets(list_buckets_request).getItems()]
    # end::method[]
