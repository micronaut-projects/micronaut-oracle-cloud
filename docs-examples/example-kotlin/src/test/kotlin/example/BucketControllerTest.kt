package example

import com.oracle.bmc.ons.responses.PublishMessageResponse
import example.mock.MockData
import io.micronaut.context.annotation.Requires
import io.micronaut.http.client.annotation.Client
import io.micronaut.oraclecloud.notifications.OracleCloudNotificationService
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@MicronautTest
@Requires(missingProperty = "micronaut.test.server.executable")
class BookControllerTest {

    @Inject
    lateinit var client: BucketClient

    @Test
    fun testBuckets() {
        MockData.bucketNames.add("b1")
        MockData.bucketNames.add("b2")
        val bucketName = "test-bucket-" + RandomStringUtils.secure().nextAlphabetic(10)

        val names = client.listBuckets(null).block()
        assertEquals(listOf("b1", "b2"), names)

        val location = client.createBucket(bucketName).block()
        assertEquals(MockData.bucketLocation, location)

        val result = client.deleteBucket(bucketName).block()
        assertTrue(result!!)
    }

    @Test
    fun testNotifications() {
        val notificationService = RecordingNotificationService()
        val controller = NotificationController(notificationService)
        val message = NotificationController.NotificationMessage("Test title", "Test body")

        controller.publish(message)
        controller.publish("ocid1.onstopic.oc1.phx.test", message)

        assertTrue(notificationService.defaultTopicPublished)
        assertTrue(notificationService.explicitTopicPublished)
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

    private class RecordingNotificationService : OracleCloudNotificationService(null, null, null) {
        var defaultTopicPublished = false
            private set
        var explicitTopicPublished = false
            private set

        override fun publish(title: String, body: String): PublishMessageResponse {
            defaultTopicPublished = title == "Test title" && body == "Test body"
            return PublishMessageResponse.builder().build()
        }

        override fun publish(topicId: String, title: String, body: String): PublishMessageResponse {
            explicitTopicPublished = topicId == "ocid1.onstopic.oc1.phx.test" && title == "Test title" && body == "Test body"
            return PublishMessageResponse.builder().build()
        }
    }
}
