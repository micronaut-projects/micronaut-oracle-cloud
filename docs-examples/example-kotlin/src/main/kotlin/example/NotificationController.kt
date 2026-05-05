package example

// tag::imports[]
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.oraclecloud.notifications.OracleCloudNotificationService
import io.micronaut.serde.annotation.Serdeable
// end::imports[]

// tag::class[]
@Controller("/notifications")
class NotificationController(
    private val notificationService: OracleCloudNotificationService // <1>
) {
    @Post
    fun publish(@Body message: NotificationMessage) {
        notificationService.publish(message.title, message.body)
    }
// end::class[]

    // tag::explicit-topic[]
    fun publish(topicId: String, message: NotificationMessage) {
        notificationService.publish(topicId, message.title, message.body) // <1>
    }
    // end::explicit-topic[]

// tag::class[]
    @Serdeable
    data class NotificationMessage(val title: String, val body: String)
}
// end::class[]
