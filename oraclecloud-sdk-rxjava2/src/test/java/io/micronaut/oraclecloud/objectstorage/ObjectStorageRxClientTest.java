package io.micronaut.oraclecloud.objectstorage;

import com.oracle.bmc.auth.AuthCachingPolicy;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import io.micronaut.oraclecloud.clients.rxjava2.objectstorage.ObjectStorageRxClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class ObjectStorageRxClientTest {

    @Test
    void testObjectStorageClientRx(ObjectStorageRxClient objectStorageRxClient) {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder()
                .namespaceName("kg")
                .compartmentId("test")
                .build();
        final List<BucketSummary> items = objectStorageRxClient
                .listBuckets(listBucketsRequest)
                .blockingGet()
                .getItems();

        List<String> names = items.stream().map(BucketSummary::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("b1", "b2"), names);
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
