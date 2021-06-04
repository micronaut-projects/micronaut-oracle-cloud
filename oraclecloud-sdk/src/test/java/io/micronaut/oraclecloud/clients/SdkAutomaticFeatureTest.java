package io.micronaut.oraclecloud.clients;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.Bucket;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SdkAutomaticFeatureTest {
    @Test
    void testPopulateReflectionData() {
        Set<Class<?>> reflectiveAccess = new HashSet<>();
        SdkAutomaticFeature.populateReflectionData(reflectiveAccess, ObjectStorage.class);

        assertTrue(reflectiveAccess.contains(Bucket.Builder.class));
    }
}
