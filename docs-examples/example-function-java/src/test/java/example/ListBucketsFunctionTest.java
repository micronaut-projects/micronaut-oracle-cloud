package example;

import com.fnproject.fn.testing.FnTestingRule;
import example.mock.MockData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.JRE.JAVA_8;

@DisabledOnJre(value = JAVA_8, disabledReason = "FDK requires Java 11+")
public class ListBucketsFunctionTest {

    FnTestingRule fn = FnTestingRule.createDefault();

    @Test
    void testFunction() {

        MockData.bucketNames.clear();
        MockData.bucketNames.add("b1");
        MockData.bucketNames.add("b2");

        fn.givenEvent().enqueue()
          .addSharedClass(MockData.class)
          .thenRun(ListBucketsFunction.class, "handleRequest");

        String body = fn.getOnlyResult().getBodyAsString();

        assertTrue(body.contains("\"kg\"") || // native image tests
                body.contains("[\"b1\",\"b2\"]")); // unit tests
    }
}
