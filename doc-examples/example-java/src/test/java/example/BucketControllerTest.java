package example;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class BucketControllerTest {
    private final BucketClient client;

    public BucketControllerTest(BucketClient client) {
        this.client = client;
    }

    @Test
    void testListBuckets() {
        List<String> names = client.listBuckets().toList().blockingGet();
        assertTrue(names.isEmpty());
    }

    @Client("/os")
    interface BucketClient {

        @Get("/buckets")
        Flowable<String> listBuckets();
    }
}
