package io.micronaut.oraclecloud.clients;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.Bucket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class SdkAutomaticFeatureTest {
    @Test
    void testPopulateReflectionData() {
        HashSet<Class<?>> reflectiveAccess = new HashSet<>();
        SdkAutomaticFeature.populateReflectionData(reflectiveAccess, ObjectStorage.class);

        Assertions.assertTrue(reflectiveAccess.contains(Bucket.Builder.class));
    }
}
