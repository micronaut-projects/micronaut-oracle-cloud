package example;

import example.mock.MockData;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Single;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
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
        List<String> names = client.listBuckets(null).blockingGet();
        assertEquals(Arrays.asList("b1", "b2"), names);

        String location = client.createBucket(bucketName).blockingGet();
        assertEquals(MockData.bucketLocation, location);

        boolean result  = client.deleteBucket(bucketName).blockingGet();
        assertTrue(result);
    }

    @AfterEach
    void cleanup() {
        MockData.reset();
    }

    @Client("/os")
    interface BucketClient extends BucketOperations {
        @Override
        Single<String> createBucket(String name);

        @Override
        Single<Boolean> deleteBucket(String name);
    }
}
