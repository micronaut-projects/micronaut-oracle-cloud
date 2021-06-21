package example

import io.micronaut.http.client.annotation.Client
import io.reactivex.Single

@Client('/os')
interface BucketClient extends BucketOperations {
    @Override
    Single<String> createBucket(String name)

    @Override
    Single<Boolean> deleteBucket(String name)
}
