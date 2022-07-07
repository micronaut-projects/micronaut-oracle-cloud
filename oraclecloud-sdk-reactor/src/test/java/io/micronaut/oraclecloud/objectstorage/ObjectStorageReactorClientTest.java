package io.micronaut.oraclecloud.objectstorage;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.oracle.bmc.auth.AuthCachingPolicy;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.responses.AsyncHandler;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.oraclecloud.clients.reactor.objectstorage.ObjectStorageReactorClient;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class ObjectStorageReactorClientTest {

    @Test
    void testObjectStorageClientRx(ObjectStorageReactorClient objectStorageRxClient) {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName("kg")
                .compartmentId("test")
                .build();
        final List<BucketSummary> items = objectStorageRxClient
                .listBuckets(listBucketsRequest)
                .block()
                .getItems();

        List<String> names = items.stream().map(BucketSummary::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("b1", "b2"), names);
    }

    @Bean
    @Replaces(BasicAuthenticationDetailsProvider.class)
    MockAuth authenticationDetailsProvider() {
        return new MockAuth();
    }

    @MockBean(ObjectStorageAsyncClient.class)
    ObjectStorageAsyncClient asyncClient() {
        final BasicAuthenticationDetailsProvider authenticationDetailsProvider = new MockAuth();
        return new ObjectStorageAsyncClient(authenticationDetailsProvider) {
            @Override
            public Future<ListBucketsResponse> listBuckets(ListBucketsRequest request,
                                                           AsyncHandler<ListBucketsRequest, ListBucketsResponse> handler) {
                final ListBucketsResponse response = ListBucketsResponse.builder()
                        .items(Arrays.asList(
                                BucketSummary.builder().name("b1").build(),
                                BucketSummary.builder().name("b2").build()
                        ))
                        .build();
                handler.onSuccess(
                        request,
                        response
                );
                return CompletableFuture.completedFuture(response);
            }
        };
    }

    @AuthCachingPolicy(cacheKeyId = false, cachePrivateKey = false)
    private static class MockAuth implements BasicAuthenticationDetailsProvider {
        @Override
        public String getKeyId() {
            return null;
        }

        @Override
        public InputStream getPrivateKey() {
            return null;
        }

        @Override
        public String getPassPhrase() {
            return null;
        }

        @Override
        public char[] getPassphraseCharacters() {
            return new char[0];
        }
    }
}
