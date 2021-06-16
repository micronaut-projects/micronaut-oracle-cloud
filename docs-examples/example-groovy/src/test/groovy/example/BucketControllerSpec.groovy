package example

import example.mock.MockData
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
class BucketControllerSpec extends Specification {

    @Inject BucketClient client

    void 'test buckets'() {
        given:
        MockData.bucketNames << 'b1' << 'b2'
        String bucketName = 'test-bucket-' + RandomStringUtils.randomAlphanumeric(10)

        when:
        List<String> names = client.listBuckets(null).blockingGet()

        then:
        names == ['b1', 'b2']

        when:
        String location = client.createBucket(bucketName).blockingGet()

        then:
        location == MockData.bucketLocation

        when:
        boolean result = client.deleteBucket(bucketName).blockingGet()

        then:
        result
    }

    void cleanup() {
        MockData.reset()
    }
}
