package example

import example.mock.MockData
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.oraclecloud.function.http.test.FnHttpTest
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Stepwise

import static io.micronaut.http.HttpStatus.OK
import static org.junit.jupiter.api.Assertions.assertEquals

@MicronautTest
@Stepwise
// avoid running this test in parallel as the interactions with Object Storage
// can step on each other causing issues
@IgnoreIf({ jvm.java8 }) // FDK requires Java 11+
class BucketControllerSpec extends Specification {

    private static final String testBucket = '__mn_oci_test_bucket'
    private static final String createDeleteUri = '/os/buckets/' + testBucket
    private static final List<Class<?>> SHARED_CLASSES = [MockData]

    void 'test list buckets'() {
        when:
        MockData.bucketNames << 'b1' << 'b2'
        HttpResponse<String> response = FnHttpTest.invoke(
                HttpRequest.GET('/os/buckets'), SHARED_CLASSES)

        then:
        response.status() == OK
        response.body() == '["b1","b2"]'
    }

    void 'test create bucket'() {
        when:
        HttpResponse<String> response = FnHttpTest.invoke(
                HttpRequest.POST(createDeleteUri, ''), SHARED_CLASSES)

        then:
        response.status() == OK
        response.body() == MockData.bucketLocation
    }

    void 'test list objects'() {
        when:
        MockData.objectNames << 'o1' << 'o2'
        HttpResponse<String> response = FnHttpTest.invoke(
                HttpRequest.GET('/os/objects/' + testBucket), SHARED_CLASSES)

        then:
        response.status() == OK
        response.body() == '{"objects":["o1","o2"]}'
    }

    void 'test delete bucket'() {
        when:
        HttpResponse<String> response = FnHttpTest.invoke(
                HttpRequest.DELETE(createDeleteUri), SHARED_CLASSES)

        then:
        response.status() == OK
        response.body() == 'true'
    }

    void cleanup() {
        MockData.reset()
    }
}
