package io.micronaut.oraclecloud.function;

import com.fnproject.fn.testing.FnTestingRule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectStorageFunctionTest {

    @Test
    void testObjectStorageFunction() {
        FnTestingRule fn = newFnRule();
        fn.givenEvent().enqueue();
        fn.thenRun(ObjectStorageFunction.class, "handleRequest");
        String result = fn.getOnlyResult().getBodyAsString();
        assertEquals("ok", result);
    }

    @NotNull
    private FnTestingRule newFnRule() {
        FnTestingRule fn = FnTestingRule.createDefault();
        fn.addSharedClassPrefix("org.slf4j.");
        fn.addSharedClassPrefix("com.sun.");
        return fn;
    }
}
