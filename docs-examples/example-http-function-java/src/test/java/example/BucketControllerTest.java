package example;

import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.oci.function.http.test.FnHttpTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BucketControllerTest {

    @Test
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
}
