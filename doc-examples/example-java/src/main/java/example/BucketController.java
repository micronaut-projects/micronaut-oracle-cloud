/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.responses.AsyncHandler;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

@Controller("/os")
public class BucketController {
    private final ObjectStorageAsync objectStorage;
    private final AuthenticationDetailsProvider detailsProvider;

    public BucketController(ObjectStorageAsync objectStorage, AuthenticationDetailsProvider detailsProvider) {
        this.objectStorage = objectStorage;
        this.detailsProvider = detailsProvider;
    }

    @Get("/buckets")
    Flowable<String> listBuckets() {
        return Flowable.create(emitter -> {
            final ListBucketsRequest.Builder builder = ListBucketsRequest.builder();
            builder.namespaceName("kg");
            builder.compartmentId(detailsProvider.getTenantId());
            objectStorage.listBuckets(builder.build(), new AsyncHandler<ListBucketsRequest, ListBucketsResponse>() {
                @Override
                public void onSuccess(
                        ListBucketsRequest request,
                        ListBucketsResponse response) {
                    response.getItems().stream()
                            .map(BucketSummary::getName)
                            .forEach(emitter::onNext);
                    emitter.onComplete();
                }

                @Override
                public void onError(ListBucketsRequest request, Throwable error) {
                    emitter.onError(error);
                }
            });
        }, BackpressureStrategy.BUFFER);
    }
}
