package example;

import example.mock.MockData;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.client.annotation.Client;
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
}
