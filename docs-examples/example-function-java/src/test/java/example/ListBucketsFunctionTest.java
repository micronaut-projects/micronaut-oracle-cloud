package example;

import com.fnproject.fn.testing.FnTestingRule;
import example.mock.MockData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("[\"b1\",\"b2\"]", body);
    }
}
