package example;

import example.mock.MockData;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.oraclecloud.function.http.test.FnHttpTest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.Collections;
import java.util.List;

import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// avoid running this test in parallel as the interactions with Object Storage
// can step on each other causing issues
@DisabledForJreRange(max = JRE.JAVA_9)
public class BucketControllerTest {

    private static final String testBucket = "__mn_oci_test_bucket";
    private static final String createDeleteUri = "/os/buckets/" + testBucket;
    private static final List<Class<?>> SHARED_CLASSES = Collections.singletonList(MockData.class);

    @Test
    @Order(1)
    void testListBuckets() {

        MockData.bucketNames.add("b1");
        MockData.bucketNames.add("b2");

        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.GET("/os/buckets"), SHARED_CLASSES);

        assertEquals(OK, response.status());
        assertEquals("[\"b1\",\"b2\"]", response.body());
    }

    @Test
    @Order(2)
    void testCreateBucket() {
        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.POST(createDeleteUri, ""), SHARED_CLASSES);

        assertEquals(OK, response.status());
        assertEquals(MockData.bucketLocation, response.body());
    }

    @Test
    @Order(3)
    void testListObjects() {

        MockData.objectNames.add("o1");
        MockData.objectNames.add("o2");

        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.GET("/os/objects/" + testBucket), SHARED_CLASSES);

        assertEquals(OK, response.status());
        assertEquals("{\"objects\":[\"o1\",\"o2\"]}", response.body());
    }

    @Test
    @Order(4)
    void testDeleteBucket() {
        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.DELETE(createDeleteUri), SHARED_CLASSES);

        assertEquals(OK, response.status());
        assertEquals("true", response.body());
    }

    @AfterEach
    void cleanup() {
        MockData.reset();
    }
}
