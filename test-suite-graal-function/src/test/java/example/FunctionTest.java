package example;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class FunctionTest {

    @Inject
    ApplicationContext context;

    @Test
    void testFunction() {
        Assertions.assertTrue(context.isRunning());
    }

}
