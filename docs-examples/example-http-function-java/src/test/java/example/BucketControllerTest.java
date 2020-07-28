package example;

import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.oci.function.http.test.FnHttpTest;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.*;

@MicronautTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BucketControllerTest {

    private final String testBucket = "__mn_oci_test_bucket";
    private final String createDeleteUri = "/os/buckets/" + testBucket;

    @Test
    @Order(1)
    void testListBuckets() {
        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.GET("/os/buckets"));

        Assertions.assertEquals(
                HttpStatus.OK,
                response.status()
        );

        Assertions.assertTrue(
                StringUtils.isNotEmpty(response.body())
        );
    }

    @Test
    @Order(2)
    void testCreateBucket() {
        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.POST(createDeleteUri, ""));

        Assertions.assertEquals(
                HttpStatus.OK,
                response.status()
        );

        Assertions.assertTrue(
                StringUtils.isNotEmpty(response.body())
        );
    }

    @Test
    @Order(3)
    void testListObjects() {
        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.GET("/os/objects/" + testBucket));

        Assertions.assertEquals(
                HttpStatus.OK,
                response.status()
        );

        Assertions.assertTrue(
                StringUtils.isNotEmpty(response.body())
        );
    }

    @Test
    @Order(4)
    void testDeleteBucket() {
        final HttpResponse<String> response = FnHttpTest
                .invoke(HttpRequest.DELETE(createDeleteUri));

        Assertions.assertEquals(
                HttpStatus.OK,
                response.status()
        );

        Assertions.assertTrue(
                StringUtils.isNotEmpty(response.body())
        );
    }
}
