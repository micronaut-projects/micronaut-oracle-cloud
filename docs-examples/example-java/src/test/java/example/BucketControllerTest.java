package example;

import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Single;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class BucketControllerTest {

    private final BucketClient client;

    public BucketControllerTest(BucketClient client) {
        this.client = client;
    }

    @Test
    void testBuckets() {
        String bucketName = "test-bucket-" + RandomStringUtils.randomAlphanumeric(10);
        List<String> names = client.listBuckets(null).blockingGet();
        assertFalse(names.isEmpty());

        String location = client.createBucket(bucketName).blockingGet();

        assertNotNull(location);

        boolean result  = client.deleteBucket(bucketName).blockingGet();
        assertTrue(result);
    }

    @Client("/os")
    interface BucketClient extends BucketOperations {
        @Override
        Single<String> createBucket(String name);

        @Override
        Single<Boolean> deleteBucket(String name);
    }
}
