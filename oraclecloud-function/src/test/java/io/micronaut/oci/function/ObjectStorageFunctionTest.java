package io.micronaut.oci.function;

import com.fnproject.fn.testing.FnTestingRule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ObjectStorageFunctionTest {

    @Test
    void testObjectStorageFunction() {
        FnTestingRule fn = newFnRule();
        fn.givenEvent().enqueue();
        fn.thenRun(ObjectStorageFunction.class, "handleRequest");
        String result = fn.getOnlyResult().getBodyAsString();
        Assertions.assertEquals("ok", result);
    }

    @NotNull
    private FnTestingRule newFnRule() {
        FnTestingRule fn = FnTestingRule.createDefault();
        fn.addSharedClassPrefix("org.slf4j.");
        fn.addSharedClassPrefix("com.sun.");
        return fn;
    }
}
