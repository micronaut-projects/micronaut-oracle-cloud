package example;

import com.fnproject.fn.testing.FnTestingRule;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
