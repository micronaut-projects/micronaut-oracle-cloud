# tag::imports[]
import os
from typing import Annotated

from com.oracle.bmc.objectstorage.model import CreateBucketDetails
from com.oracle.bmc.objectstorage.requests import CreateBucketRequest, DeleteBucketRequest, GetNamespaceRequest, \
    ListBucketsRequest
from jakarta.annotation import Nullable
from java.util.stream import Collectors
from micronaut.core.async_.annotation import SingleResult
from micronaut.http.annotation import Controller, Delete, Get, PathVariable, Post
from micronaut.oraclecloud.clients.reactor.objectstorage import ObjectStorageReactorClient
from micronaut.oraclecloud.core import TenancyIdProvider
from org.reactivestreams import Publisher
# end::imports[]


# tag::class[]
@Controller("/os")
class BucketController:

    def __init__(self, object_storage: ObjectStorageReactorClient,
                 tenancy_id_provider: TenancyIdProvider):  # <1>
        self.object_storage = object_storage
        self.tenancy_id_provider = tenancy_id_provider
        self.default_compartment_id = os.environ.get("COMPARTMENT_OCID")
# end::class[]

    @Get("/buckets{/compartmentId}")
    @SingleResult
    def list_buckets(self, compartmentId: Annotated[str | None, PathVariable, Nullable]) -> Publisher[list[str]]:
        compartment_id = compartmentId if compartmentId is not None else self.default_compartment_id
        compartment_ocid = compartment_id if compartment_id is not None else self.tenancy_id_provider.getTenancyId()

        get_namespace_request = GetNamespaceRequest.builder().compartmentId(compartment_ocid).build()

        def list_buckets(namespace_response):
            list_buckets_request = (ListBucketsRequest.builder()
                                    .namespaceName(namespace_response.getValue())
                                    .compartmentId(compartment_ocid)
                                    .build())
            return (self.object_storage.listBuckets(list_buckets_request)
                    .map(lambda list_buckets_response: list_buckets_response.getItems()
                         .stream()
                         .map(lambda bucket: bucket.getName())
                         .collect(Collectors.toList())))

        return self.object_storage.getNamespace(get_namespace_request).flatMap(list_buckets)

    # tag::method[]
    @Post("/buckets/{name}")
    @SingleResult
    def create_bucket(self, name: str) -> Publisher[str]:

        compartment_id = self.default_compartment_id if self.default_compartment_id is not None \
            else self.tenancy_id_provider.getTenancyId()

        get_namespace_request = GetNamespaceRequest.builder().compartmentId(compartment_id).build()
        namespace = self.object_storage.getNamespace(get_namespace_request)  # <1>

        def create_bucket(namespace_response):
            create_bucket_request = (CreateBucketRequest.builder()
                                     .namespaceName(namespace_response.getValue())
                                     .createBucketDetails(CreateBucketDetails.builder()
                                                          .compartmentId(compartment_id)
                                                          .name(name)
                                                          .build())
                                     .build())

            return (self.object_storage.createBucket(create_bucket_request)  # <2>
                    .map(lambda create_bucket_response: create_bucket_response.getLocation()))  # <3>

        return namespace.flatMap(create_bucket)
    # end::method[]

    @Delete("/buckets/{name}")
    @SingleResult
    def delete_bucket(self, name: str) -> Publisher[bool]:
        compartment_id = self.default_compartment_id if self.default_compartment_id is not None \
            else self.tenancy_id_provider.getTenancyId()

        get_namespace_request = GetNamespaceRequest.builder().compartmentId(compartment_id).build()

        def delete_bucket(get_namespace_response):
            delete_bucket_request = (DeleteBucketRequest.builder()
                                     .namespaceName(get_namespace_response.getValue())
                                     .bucketName(name)
                                     .build())

            return self.object_storage.deleteBucket(delete_bucket_request).map(lambda response: True)

        return self.object_storage.getNamespace(get_namespace_request).flatMap(delete_bucket)
