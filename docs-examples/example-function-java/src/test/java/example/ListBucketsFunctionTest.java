package example;

import com.fnproject.fn.testing.FnTestingRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

public class ListBucketsFunctionTest {

    @Test
    @DisabledOnJre(value=JRE.JAVA_8, disabledReason = "Possible bug in FDK means this requires Java 11")
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
