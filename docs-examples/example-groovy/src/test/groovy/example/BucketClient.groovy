package example

import io.micronaut.http.client.annotation.Client
import reactor.core.publisher.Mono

@Client('/os')
interface BucketClient extends BucketOperations {
    @Override
    Mono<String> createBucket(String name)

    @Override
    Mono<Boolean> deleteBucket(String name)
}
