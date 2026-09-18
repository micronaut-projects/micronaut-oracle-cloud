from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from mock import MockData
from org.junit.jupiter.api import Test

from example.ListBucketsFunction import ListBucketsFunction


# The Fn runtime instantiates the function class itself, so unlike the Java test this test does not run the
# function through the Fn testing harness (com.fnproject.fn.testing.FnTestingRule): a Python function class is a
# bean of the running application context. The OCI Object Storage client and the authentication are replaced by
# the Java mocks of src/test/java (package mock).
@MicronautTest(environments=["function", "oraclecloud"])
class ListBucketsFunctionTest:
    function: Annotated[ListBucketsFunction, Inject]

    @Test
    def test_function(self) -> None:
        MockData.bucketNames.clear()
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")

        assert self.function.handle_request() == ["b1", "b2"]
