import uuid
from typing import Annotated

from jakarta.inject import Inject
from micronaut.http.annotation import Delete, Get, Post
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from mock import MockData
from org.junit.jupiter.api import AfterEach, Test
from org.reactivestreams import Publisher
from reactor.core.publisher import Mono


@Client("/os")
class BucketClient:

    @Get("/buckets{/compartmentId}")
    def list_buckets(self, compartmentId: str | None) -> Publisher[list[str]]: ...

    @Post("/buckets/{name}")
    def create_bucket(self, name: str) -> Publisher[str]: ...

    @Delete("/buckets/{name}")
    def delete_bucket(self, name: str) -> Publisher[bool]: ...


# The OCI Object Storage client and authentication are replaced by the Java mocks of src/test/java (package mock)
@MicronautTest
class BucketControllerTest:
    client: Annotated[BucketClient, Inject]

    @Test
    def test_buckets(self) -> None:
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")

        bucket_name = "test-bucket-" + uuid.uuid4().hex[:10]

        names = Mono.from_(self.client.list_buckets(None)).block()
        assert not names.isEmpty()

        location = Mono.from_(self.client.create_bucket(bucket_name)).block()
        assert location is not None

        result = Mono.from_(self.client.delete_bucket(bucket_name)).block()
        assert result

    @AfterEach
    def cleanup(self) -> None:
        MockData.reset()
