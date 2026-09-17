package support;

import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.testing.FnEventBuilder;
import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.oraclecloud.function.http.HttpFunction;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Invokes the HTTP function through the Fn testing harness ({@link FnTestingRule}), like
 * {@code io.micronaut.oraclecloud.function.http.test.FnHttpTest} does for the Java example.
 * <p>
 * TODO(python): {@code FnHttpTest.invoke} starts and closes a nested {@code ApplicationContext} to serialize the
 * request body, which tears down the GraalPy runtime shared with the application context of the running Python
 * test, so the Python test uses this helper, which only supports text request bodies.
 */
public final class FnHttpInvoker {

    private FnHttpInvoker() {
    }

    /**
     * Invoke the function via HTTP.
     *
     * @param request       The request (with an optional text body)
     * @param sharedClasses The classes to share between the test classloader and the Fn classloader
     * @return The response
     */
    public static Response invoke(HttpRequest<?> request, List<Class<?>> sharedClasses) {
        FnTestingRule fn = FnTestingRule.createDefault();
        fn.addSharedClassPrefix("org.slf4j.");
        fn.addSharedClassPrefix("com.sun.");
        for (Class<?> c : sharedClasses) {
            fn.addSharedClass(c);
        }
        FnEventBuilder<FnTestingRule> eventBuilder = fn.givenEvent()
            .withHeader("Fn-Http-Request-Url", request.getUri().toString())
            .withHeader("Fn-Http-Method", request.getMethodName());
        request.getHeaders().forEach((name, values) -> {
            for (String value : values) {
                eventBuilder.withHeader("Fn-Http-H-" + name, value);
            }
        });
        request.getBody().ifPresent(body ->
            eventBuilder.withBody(body.toString().getBytes(StandardCharsets.UTF_8)));
        eventBuilder.enqueue();
        fn.thenRun(HttpFunction.class, "handleRequest");
        FnResult result = fn.getOnlyResult();
        HttpStatus status = result.getHeaders().get("Fn-Http-Status")
            .map(s -> HttpStatus.valueOf(Integer.parseInt(s)))
            .orElse(result.getStatus() == OutputEvent.Status.Success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
        return new Response(status, result.getBodyAsString());
    }

    /**
     * The status and body of the function response.
     *
     * @param status The status
     * @param body   The body
     */
    public record Response(HttpStatus status, String body) {
    }
}
