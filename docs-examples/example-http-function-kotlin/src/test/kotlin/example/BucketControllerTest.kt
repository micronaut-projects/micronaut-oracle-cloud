package example

import example.mock.MockData
import example.mock.MockData.reset
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus.OK
import io.micronaut.oraclecloud.function.http.test.FnHttpTest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

// avoid running this test in parallel as the interactions with Object Storage
// can step on each other causing issues
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@MicronautTest
class BucketControllerTest {

    @Test
    @Order(1)
    fun testListBuckets() {
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")

        val response = FnHttpTest
            .invoke(HttpRequest.GET("/os/buckets"), SHARED_CLASSES)

        assertEquals(OK, response.status())
        assertEquals("[\"b1\",\"b2\"]", response.body())
    }

    @Test
    @Order(2)
    fun testCreateBucket() {
        val response: HttpResponse<String> = FnHttpTest
            .invoke(HttpRequest.POST(CREATE_DELETE_URI, ""), SHARED_CLASSES)

        assertEquals(OK, response.status())
        assertEquals(MockData.bucketLocation, response.body())
    }

    @Test
    @Order(3)
    fun testListObjects() {
        MockData.objectNames.add("o1")
        MockData.objectNames.add("o2")

        val response = FnHttpTest
            .invoke(HttpRequest.GET("/os/objects/$TEST_BUCKET"), SHARED_CLASSES)

        assertEquals(OK, response.status())
        assertTrue(response.body()!!.contains("\"objects\":[\"o1\",\"o2\"]"))
    }

    @Test
    @Order(4)
    fun testDeleteBucket() {
        val response: HttpResponse<String> = FnHttpTest
            .invoke(HttpRequest.DELETE(CREATE_DELETE_URI), SHARED_CLASSES)

        assertEquals(OK, response.status())
        assertEquals("true", response.body())
    }

    @AfterEach
    fun cleanup() {
        reset()
    }

    companion object {
        private const val TEST_BUCKET = "__mn_oci_test_bucket"
        private const val CREATE_DELETE_URI = "/os/buckets/$TEST_BUCKET"
        private val SHARED_CLASSES = mutableListOf<Class<*>?>(MockData::class.java)
    }
}
