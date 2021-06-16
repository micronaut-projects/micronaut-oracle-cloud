package io.micronaut.oraclecloud.function;

import com.fnproject.fn.testing.FnTestingRule;
import io.micronaut.oraclecloud.function.mock.MockData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectStorageFunctionTest {

    @Test
    void testObjectStorageFunction() {

        MockData.bucketNames.clear();
        MockData.bucketNames.add("b1");
        MockData.bucketNames.add("b2");

        FnTestingRule fn = FnTestingRule.createDefault();
        fn.addSharedClassPrefix("org.slf4j.");
        fn.addSharedClassPrefix("com.sun.");
        fn.addSharedClass(MockData.class);

        fn.givenEvent().enqueue();
        fn.thenRun(ObjectStorageFunction.class, "handleRequest");

        String result = fn.getOnlyResult().getBodyAsString();
        assertEquals("[b1, b2]", result);
    }
}
