package example

import example.mock.MockData
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.util.*

@MicronautTest
class BookControllerTest {
    @Inject
    lateinit var client: BucketClient

    @Test
    fun testBuckets() {
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")
        val bucketName = "test-bucket-" + RandomStringUtils.randomAlphanumeric(10)
        val names = client!!.listBuckets(null).block()
        Assertions.assertEquals(Arrays.asList("b1", "b2"), names)
        val location = client.createBucket(bucketName).block()
        Assertions.assertEquals(MockData.bucketLocation, location)
        val result = client.deleteBucket(bucketName).block()
        Assertions.assertTrue(result)
    }

    @AfterEach
    fun cleanup() {
        MockData.reset()
    }

    @Client("/os")
    interface BucketClient : BucketOperations {
        override fun createBucket(name: String): Mono<String>
        override fun deleteBucket(name: String): Mono<Boolean>
    }
}
