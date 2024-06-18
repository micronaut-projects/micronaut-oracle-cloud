package io.micronaut.oraclecloud.function.http

import com.fnproject.fn.api.Headers
import com.fnproject.fn.api.InvocationContext
import com.fnproject.fn.api.QueryParameters
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext
import com.fnproject.fn.runtime.FunctionRuntimeContext
import com.fnproject.fn.runtime.ReadOnceInputEvent
import com.fnproject.fn.runtime.httpgateway.QueryParametersImpl
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.body.ByteBody
import io.micronaut.http.body.CloseableByteBody
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.apache.commons.io.input.CountingInputStream
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

@MicronautTest
@Property(name = "spec.name", value = "HttpFunctionBufferingSpec")
class HttpFunctionBufferingSpec extends Specification {
    @Inject
    HttpFunction httpFunction
    @Inject
    Ctrl ctrl

    def setup() {
        httpFunction.setup(new FunctionRuntimeContext(null, new HashMap<>()))
    }

    private def send(String path, InputStream body) {
        def evt = new ReadOnceInputEvent(body, Headers.emptyHeaders(), "", Instant.MAX)
        def outputEvent = httpFunction.handleRequest(new MockContext(path, "POST"), evt)
        def out = new ByteArrayOutputStream()
        outputEvent.writeToOutput(out)
        return out.toString(StandardCharsets.UTF_8)
    }

    def unused() {
        given:
        def counter = new CountingInputStream(new ByteArrayInputStream(new byte[100]))
        expect:
        send("/buffering/unused", counter) == "foo"
        counter.byteCount == 0L
    }

    def splitUsedLater() {
        given:
        def bytes = new byte[100]
        ThreadLocalRandom.current().nextBytes(bytes)
        def counter = new CountingInputStream(new ByteArrayInputStream(bytes))
        expect:
        send("/buffering/splitUsedLater", counter) == "foo"
        counter.byteCount == 100L
        Arrays.equals(ctrl.split.buffer().get().toByteArray(), bytes)
    }

    def splitClosedHalfway() {
        given:
        def bytes = new byte[16384]
        ThreadLocalRandom.current().nextBytes(bytes)
        def counter = new CountingInputStream(new ByteArrayInputStream(bytes))
        expect:
        send("/buffering/splitClosedHalfway", counter) == "foo"
        counter.byteCount == 8192L // fnproject uses BufferedInputStream so we see more than expected here
        Arrays.equals(ctrl.seenData, 0, 100, bytes, 0, 100)
    }

    def splitDiscarded() {
        given:
        def counter = new CountingInputStream(new ByteArrayInputStream(new byte[100]))
        expect:
        send("/buffering/splitDiscarded", counter) == "foo"
        counter.byteCount == 0L

        when:
        ctrl.split.toInputStream().readAllBytes()
        then:
        thrown ByteBody.BodyDiscardedException
    }

    @Controller("/buffering")
    @Requires(property = "spec.name", value = "HttpFunctionBufferingSpec")
    static class Ctrl {
        @Post("/unused")
        String unused() {
            return "foo"
        }

        CloseableByteBody split

        @Post("/splitUsedLater")
        String splitUsedLater(HttpRequest<?> request) {
            // ServletRequestAndBody does not currently support byteBody. It should in the future
            split = request.getDelegate().byteBody().split()
            return "foo"
        }

        byte[] seenData

        @Post("/splitClosedHalfway")
        String splitClosedHalfway(HttpRequest<?> request) {
            try (InputStream body = request.getDelegate().byteBody().toInputStream()) {
                seenData = body.readNBytes(100)
            }
            return "foo"
        }

        @Post("/splitDiscarded")
        String splitDiscarded(HttpRequest<?> request) {
            // ServletRequestAndBody does not currently support byteBody. It should in the future
            split = request.getDelegate().byteBody().split().allowDiscard()
            return "foo"
        }
    }

    static record MockContext(
            String requestUrl,
            String method
    ) implements HTTPGatewayContext {

        @Override
        InvocationContext getInvocationContext() {
            throw new UnsupportedOperationException()
        }

        @Override
        Headers getHeaders() {
            return Headers.emptyHeaders()
        }

        @Override
        String getRequestURL() {
            return requestUrl
        }

        @Override
        String getMethod() {
            return method
        }

        @Override
        QueryParameters getQueryParameters() {
            return new QueryParametersImpl()
        }

        @Override
        void addResponseHeader(String key, String value) {
        }

        @Override
        void setResponseHeader(String key, String v1, String... vs) {
        }

        @Override
        void setStatusCode(int code) {
        }
    }
}
