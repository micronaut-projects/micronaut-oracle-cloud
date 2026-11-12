package example;

import com.oracle.bmc.ons.responses.PublishMessageResponse;
import example.mock.MockData;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.oraclecloud.notifications.OracleCloudNotificationService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Requires(missingProperty = "micronaut.test.server.executable")
public class BucketControllerTest {

    private final BucketClient client;

    public BucketControllerTest(BucketClient client) {
        this.client = client;
    }

    @Test
    void testBuckets() {

        MockData.bucketNames.add("b1");
        MockData.bucketNames.add("b2");

        String bucketName = "test-bucket-" + RandomStringUtils.randomAlphanumeric(10);

        List<String> names = client.listBuckets(null).block();
        assertFalse(names.isEmpty());

        String location = client.createBucket(bucketName).block();
        assertNotNull(location);

        boolean result  = client.deleteBucket(bucketName).block();
        assertTrue(result);
    }

    @Test
    void testNotifications() {
        RecordingNotificationService notificationService = new RecordingNotificationService();
        NotificationController controller = new NotificationController(notificationService);
        NotificationController.NotificationMessage message = new NotificationController.NotificationMessage("Test title", "Test body");

        controller.publish(message);
        controller.publish("ocid1.onstopic.oc1.phx.test", message);

        assertTrue(notificationService.defaultTopicPublished);
        assertTrue(notificationService.explicitTopicPublished);
    }

    @AfterEach
    void cleanup() {
        MockData.reset();
    }

    @Client("/os")
    interface BucketClient extends BucketOperations {
        @Override
        Mono<String> createBucket(String name);

        @Override
        Mono<Boolean> deleteBucket(String name);
    }

    private static final class RecordingNotificationService extends OracleCloudNotificationService {
        private boolean defaultTopicPublished;
        private boolean explicitTopicPublished;

        private RecordingNotificationService() {
            super(null, null, null);
        }

        @Override
        public PublishMessageResponse publish(String title, String body) {
            defaultTopicPublished = "Test title".equals(title) && "Test body".equals(body);
            return PublishMessageResponse.builder().build();
        }

        @Override
        public PublishMessageResponse publish(String topicId, String title, String body) {
            explicitTopicPublished = "ocid1.onstopic.oc1.phx.test".equals(topicId) && "Test title".equals(title) && "Test body".equals(body);
            return PublishMessageResponse.builder().build();
        }
    }
}
