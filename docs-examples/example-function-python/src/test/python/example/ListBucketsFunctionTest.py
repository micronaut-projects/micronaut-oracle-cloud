from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from mock import MockData
from org.junit.jupiter.api import Test

from example.ListBucketsFunction import ListBucketsFunction


# TODO(python): unlike the Java test this test does not run the function through the Fn testing harness
# (com.fnproject.fn.testing.FnTestingRule): the harness instantiates the function class reflectively before any
# application context exists, and the generated class of the Python function has several public constructors
# ("The function class example.ListBucketsFunction cannot be instantiated as it has multiple public constructors"),
# see DISABLED_TESTS.md. The function is obtained as a bean of the running application context instead. The OCI
# Object Storage client and the authentication are replaced by the Java mocks of src/test/java (package mock).
@MicronautTest(environments=["function", "oraclecloud"])
class ListBucketsFunctionTest:
    function: Annotated[ListBucketsFunction, Inject]

    @Test
    def test_function(self) -> None:
        MockData.bucketNames.clear()
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")

        assert self.function.handle_request() == ["b1", "b2"]
