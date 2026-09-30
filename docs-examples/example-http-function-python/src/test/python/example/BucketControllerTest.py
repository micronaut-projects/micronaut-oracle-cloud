from micronaut.http import HttpRequest, HttpStatus
from micronaut.oraclecloud.function.http.test import FnHttpTest
from micronaut.test.extensions.junit5.annotation import MicronautTest
from mock import MockData
from org.junit.jupiter.api import AfterEach, MethodOrderer, Order, Test, TestMethodOrder

TEST_BUCKET = "__mn_oci_test_bucket"
DELETE_URI = "/os/buckets/" + TEST_BUCKET
SHARED_CLASSES = [MockData]


# The OCI Object Storage client and the authentication are replaced by the Java mocks of src/test/java (package mock).
@MicronautTest
@TestMethodOrder(MethodOrderer.OrderAnnotation)
# avoid running this test in parallel as the interactions with Object Storage
# can step on each other causing issues
class BucketControllerTest:

    @Test
    @Order(1)
    def test_list_buckets(self) -> None:
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")

        response = FnHttpTest.invoke(HttpRequest.GET("/os/buckets"), SHARED_CLASSES)

        assert response.status() == HttpStatus.OK
        assert response.body() == '["b1","b2"]'

    @Test
    @Order(2)
    def test_list_objects(self) -> None:
        MockData.objectNames.add("o1")
        MockData.objectNames.add("o2")

        response = FnHttpTest.invoke(HttpRequest.GET("/os/objects/" + TEST_BUCKET), SHARED_CLASSES)

        assert response.status() == HttpStatus.OK
        assert '"objects":["o1","o2"]' in response.body()

    @Test
    @Order(3)
    def test_delete_bucket(self) -> None:
        response = FnHttpTest.invoke(HttpRequest.DELETE(DELETE_URI), SHARED_CLASSES)

        assert response.status() == HttpStatus.OK
        assert response.body() == "true"

    @AfterEach
    def cleanup(self) -> None:
        MockData.reset()
