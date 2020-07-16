package example;

import com.fnproject.fn.testing.FnTestingRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.*;

public class ListBucketsFunctionTest {

    @Test
    @Disabled
    void testFunction() {
        FnTestingRule fn = newFnRule();
        fn.givenEvent().enqueue();
        fn.thenRun(ListBucketsFunction.class, "handleRequest");
        String result = fn.getOnlyResult().getBodyAsString();
        Assertions.assertEquals("[\"kg\"]", result);
    }

    private FnTestingRule newFnRule() {
        FnTestingRule fn = FnTestingRule.createDefault();
        return fn;
    }
}
