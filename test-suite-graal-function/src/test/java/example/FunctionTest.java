package example;

import com.fnproject.fn.testing.FnTestingRule;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class FunctionTest {

    FnTestingRule fn = FnTestingRule.createDefault();

    @Test
    void testFunction() {

        fn.givenEvent().enqueue()
            .thenRun(Function.class, "handleRequest");

        String body = fn.getOnlyResult().getBodyAsString();
        assertEquals("Your tenancy is: test-tenancyId", body);
    }

}
