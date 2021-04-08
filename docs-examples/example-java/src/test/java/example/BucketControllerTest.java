package example;

import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Single;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class BucketControllerTest {
    private final BucketClient client;

    public BucketControllerTest(BucketClient client) {
        this.client = client;
    }

    @Test
    void testBuckets() {
        List<String> names = client.listBuckets(null).blockingGet();
        assertFalse(names.isEmpty());

        String location = client.createBucket("test-bucket").blockingGet();

        assertNotNull(location);

        boolean result  = client.deleteBucket("test-bucket").blockingGet();
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
