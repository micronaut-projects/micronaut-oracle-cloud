# tag::imports[]
from typing import Annotated

from com.oracle.bmc.objectstorage import ObjectStorage
from com.oracle.bmc.objectstorage.model import CreateBucketDetails
from com.oracle.bmc.objectstorage.requests import CreateBucketRequest, DeleteBucketRequest, GetNamespaceRequest, \
    ListBucketsRequest, ListObjectsRequest
from jakarta.annotation import Nullable
from micronaut.http import MediaType
from micronaut.http.annotation import Controller, Delete, Get, PathVariable, Post, QueryValue
from micronaut.oraclecloud.core import TenancyIdProvider
# end::imports[]


# tag::class[]
@Controller("/os")
class BucketController:

    def __init__(self, object_storage: ObjectStorage,
                 tenancy_id_provider: TenancyIdProvider):  # <1>
        self.object_storage = object_storage
        self.tenancy_id_provider = tenancy_id_provider
# end::class[]

    # tag::listBuckets[]
    @Get("/buckets{/compartmentId}")
    def list_buckets(self, compartmentId: Annotated[str | None, PathVariable, Nullable]) -> list[str]:

        compartment_ocid = compartmentId if compartmentId is not None else self.tenancy_id_provider.getTenancyId()

        get_namespace_request = GetNamespaceRequest.builder().compartmentId(compartment_ocid).build()
        namespace = self.object_storage.getNamespace(get_namespace_request).getValue()

        list_buckets_request = (ListBucketsRequest.builder()
                                .namespaceName(namespace)
                                .compartmentId(compartment_ocid)
                                .build())

        return [bucket.getName() for bucket in
                self.object_storage.listBuckets(list_buckets_request).getItems()]
    # end::listBuckets[]

    @Post(uri="/echo/{name}", produces=MediaType.TEXT_PLAIN)
    def test_post(self, name: Annotated[str, PathVariable]) -> str:
        print("/echo as POST")
        return "Hello, " + name

    @Get(uri="/echo/{name}", produces=MediaType.TEXT_PLAIN)
    def test_get(self, name: Annotated[str, PathVariable]) -> str:
        print("/echo as GET")
        return "Hello, " + name

    @Get("/objects/{bucketName}{/start}")
    def list_objects(self, bucketName: Annotated[str, PathVariable],
                     start: Annotated[str | None, PathVariable, Nullable],
                     limit: Annotated[int | None, QueryValue]) -> dict[str, object]:

        get_namespace_request = (GetNamespaceRequest.builder()
                                 .compartmentId(self.tenancy_id_provider.getTenancyId()).build())
        namespace = self.object_storage.getNamespace(get_namespace_request).getValue()

        list_objects_request_builder = (ListObjectsRequest.builder()
                                        .bucketName(bucketName)
                                        .limit(limit if limit is not None else 25)
                                        .namespaceName(namespace))
        if start is not None:
            list_objects_request_builder.start(start)
        list_objects = self.object_storage.listObjects(list_objects_request_builder.build()).getListObjects()
        next_start = list_objects.getNextStartWith()
        objects = [summary.getName() for summary in list_objects.getObjects()]
        return {
            "nextStart": next_start if next_start is not None else "",
            "objects": objects
        }

    # tag::method[]
    @Post("/buckets/{name}")
    def create_bucket(self, name: Annotated[str, PathVariable]) -> str:

        tenancy_id = self.tenancy_id_provider.getTenancyId()

        get_namespace_request = GetNamespaceRequest.builder().compartmentId(tenancy_id).build()
        namespace = self.object_storage.getNamespace(get_namespace_request).getValue()  # <1>

        create_bucket_request = (CreateBucketRequest.builder()
                                 .namespaceName(namespace)
                                 .createBucketDetails(CreateBucketDetails.builder()
                                                      .compartmentId(tenancy_id)
                                                      .name(name)
                                                      .build())
                                 .build())

        return (self.object_storage.createBucket(create_bucket_request)  # <2>
                .getLocation())  # <3>
    # end::method[]

    @Delete("/buckets/{name}")
    def delete_bucket(self, name: Annotated[str, PathVariable]) -> bool:

        get_namespace_request = (GetNamespaceRequest.builder()
                                 .compartmentId(self.tenancy_id_provider.getTenancyId()).build())
        namespace = self.object_storage.getNamespace(get_namespace_request).getValue()

        delete_bucket_request = (DeleteBucketRequest.builder()
                                 .namespaceName(namespace)
                                 .bucketName(name)
                                 .build())

        self.object_storage.deleteBucket(delete_bucket_request)
        return True
