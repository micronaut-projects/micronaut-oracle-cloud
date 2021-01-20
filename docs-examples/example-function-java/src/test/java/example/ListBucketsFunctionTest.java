package example;

import com.fnproject.fn.testing.FnTestingRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListBucketsFunctionTest {

    @Test
    void testFunction() {
        FnTestingRule fn = newFnRule();
        fn.givenEvent().enqueue();
        fn.thenRun(ListBucketsFunction.class, "handleRequest");
        String result = fn.getOnlyResult().getBodyAsString();
        Assertions.assertTrue(result.contains("\"kg\""));
    }

    private FnTestingRule newFnRule() {
        return FnTestingRule.createDefault();
    }
}
