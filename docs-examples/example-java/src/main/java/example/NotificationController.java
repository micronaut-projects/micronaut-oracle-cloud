package example;

// tag::imports[]
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.oraclecloud.notifications.OracleCloudNotificationService;
import io.micronaut.serde.annotation.Serdeable;
// end::imports[]

// tag::class[]
@Controller("/notifications")
class NotificationController {
    private final OracleCloudNotificationService notificationService; // <1>

    NotificationController(OracleCloudNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Post
    void publish(@Body NotificationMessage message) {
        notificationService.publish(message.title(), message.body());
    }
// end::class[]

    // tag::explicit-topic[]
    @Post("/{topicId}")
    void publish(@PathVariable String topicId, @Body NotificationMessage message) {
        notificationService.publish(topicId, message.title(), message.body()); // <1>
    }
    // end::explicit-topic[]

// tag::class[]
    @Serdeable
    record NotificationMessage(String title, String body) {
    }
}
// end::class[]
