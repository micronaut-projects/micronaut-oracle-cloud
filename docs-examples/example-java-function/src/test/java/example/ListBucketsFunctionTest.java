package example;

import com.fnproject.fn.testing.FnTestingRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ListBucketsFunctionTest {

    @Test
    @Disabled // fails on JDK 8 only due to back in Fn?
    void testFunction() {
        FnTestingRule fn = newFnRule();
        fn.givenEvent().enqueue();
        fn.thenRun(ListBucketsFunction.class, "handleRequest");
        String result = fn.getOnlyResult().getBodyAsString();
        Assertions.assertEquals("[\"kg\"]", result);
    }

    private FnTestingRule newFnRule() {
        return FnTestingRule.createDefault();
    }
}
