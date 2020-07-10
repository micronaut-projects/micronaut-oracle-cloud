package io.micronaut.oci.function.http;

import com.fnproject.fn.testing.FnTestingRule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParameterBindingTest {


    @Test
    void testUriParameterBinding() {
        FnTestingRule fn = newFnRule();
        fn.givenEvent()
                .withHeader("Fn-Http-Request-Url", "/parameters/uri/Fred")
                .withHeader("Fn-Http-Method","GET")
                .enqueue();
        fn.thenRun(HttpFunction.class, "handleRequest");
        String result = fn.getOnlyResult().getBodyAsString();
        Assertions.assertEquals("Hello Fred", result);
    }


    @NotNull
    private FnTestingRule newFnRule() {
        FnTestingRule fn = FnTestingRule.createDefault();
        fn.addSharedClassPrefix("org.slf4j.");
        fn.addSharedClassPrefix("com.sun.");
        return fn;
    }
}
