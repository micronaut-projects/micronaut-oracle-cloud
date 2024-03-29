package example

import example.mock.MockData
import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

import jakarta.inject.Inject

@MicronautTest
@Requires(missingProperty = "micronaut.test.server.executable")
class BucketControllerSpec extends Specification {

    @Inject BucketClient client

    void 'test buckets'() {
        given:
        MockData.bucketNames << 'b1' << 'b2'
        String bucketName = 'test-bucket-' + RandomStringUtils.randomAlphanumeric(10)

        when:
        List<String> names = client.listBuckets(null).block()

        then:
        names == ['b1', 'b2']

        when:
        String location = client.createBucket(bucketName).block()

        then:
        location == MockData.bucketLocation

        when:
        boolean result = client.deleteBucket(bucketName).block()

        then:
        result
    }

    void cleanup() {
        MockData.reset()
    }
}
