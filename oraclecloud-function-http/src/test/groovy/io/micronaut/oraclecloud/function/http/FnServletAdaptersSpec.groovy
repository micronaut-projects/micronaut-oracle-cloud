package io.micronaut.oraclecloud.function.http

import com.fnproject.fn.api.Headers
import com.fnproject.fn.api.InvocationContext
import com.fnproject.fn.api.QueryParameters
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext
import com.fnproject.fn.runtime.ReadOnceInputEvent
import com.fnproject.fn.runtime.httpgateway.QueryParametersImpl
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.io.buffer.ByteArrayBufferFactory
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.body.ByteBody
import io.micronaut.http.body.stream.InputStreamByteBody
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.OptionalLong
import java.util.concurrent.Executors

class FnServletAdaptersSpec extends Specification {

    def conversionService = ConversionService.SHARED
    def ioExecutor = Executors.newSingleThreadExecutor()

    def cleanup() {
        ioExecutor.shutdownNow()
    }

    void "request resolves relative request url with localhost defaults"() {
        given:
        def request = newServletRequest('/relative/path?foo=bar')

        expect:
        request.uri.toString() == '/relative/path?foo=bar'
        request.serverName == 'localhost'
        request.serverAddress.hostString == 'localhost'
        request.serverAddress.port == 80
    }

    void "request resolves absolute request url authority"() {
        given:
        def request = newServletRequest('https://example.com:8443/test?q=1')

        expect:
        request.uri.toString() == 'https://example.com:8443/test?q=1'
        request.serverName == 'example.com'
        request.serverAddress.hostString == 'example.com'
        request.serverAddress.port == 8443
    }

    void "request reads normalized headers from input event and writes mutable headers to response"() {
        given:
        def response = new FnServletResponse<>(new TestGatewayContext('/headers'), conversionService)
        def event = new ReadOnceInputEvent(
            InputStream.nullInputStream(),
            Headers.fromMultiHeaderMap([
                'Fn-Http-H-Origin'                  : ['https://foo.com'],
                'Fn-Http-H-X-Forwarded-For'         : ['1.2.3.4'],
                'Fn-Http-H-Access-Control-Request-Method': ['OPTIONS']
            ]),
            'call',
            Instant.MAX
        )
        def request = new FnServletRequest<>(
            emptyByteBody(),
            event,
            response,
            new TestGatewayContext('/headers'),
            conversionService,
            null
        )

        when:
        request.headers.add('X-Test', 'value')

        then:
        request.headers.get('Origin') == 'https://foo.com'
        request.headers.get('X-Forwarded-For') == '1.2.3.4'
        request.headers.get('Access-Control-Request-Method') == 'OPTIONS'
        response.headers.get('X-Test') == 'value'
    }

    void "request resolves remote address from source ip header"() {
        given:
        def request = newServletRequest('/remote', [
            'Fn-Http-Source-Ip': ['192.168.1.10']
        ])

        expect:
        request.remoteAddress.address.hostAddress == '192.168.1.10'
    }

    void "request resolves remote address from forwarded header"() {
        given:
        def request = newServletRequest('/remote', [
            'Fn-Http-H-Forwarded': ['for="[2001:db8:cafe::17]:4711";proto=https']
        ])

        expect:
        request.remoteAddress.address.hostAddress == '2001:db8:cafe:0:0:0:0:17'
    }

    void "response preserves headers on empty body responses using synthetic transport workaround"() {
        given:
        def gatewayContext = new TestGatewayContext('/response')
        def response = new FnServletResponse<>(gatewayContext, conversionService)
        response.status(HttpStatus.NO_CONTENT.code, HttpStatus.NO_CONTENT.reason)
        response.headers.add('X-Test', 'present')

        when:
        def nativeResponse = response.nativeResponse
        def outputBytes = outputBytes(nativeResponse)

        then:
        nativeResponse.headers.get('Fn-Http-H-X-Test').get() == 'present'
        nativeResponse.headers.get('Fn-Http-H-X-Micronaut-Fn-Synthetic-Empty-Body').get() == 'true'
        nativeResponse.contentType.isPresent()
        nativeResponse.contentType.get() == 'application/json'
        new String(outputBytes, StandardCharsets.UTF_8) == ' '
    }

    void "response copies logical http response headers before serialization"() {
        given:
        def gatewayContext = new TestGatewayContext('/logical')
        def response = new FnServletResponse<>(gatewayContext, conversionService)
        response.body(HttpResponse.status(HttpStatus.CREATED)
            .contentType(MediaType.TEXT_PLAIN_TYPE)
            .header('Location', '/created')
            .body('done'))

        when:
        def nativeResponse = response.nativeResponse
        def outputBytes = outputBytes(nativeResponse)

        then:
        gatewayContext.statusCode == HttpStatus.CREATED.code
        nativeResponse.headers.get('Fn-Http-H-Location').get() == '/created'
        new String(outputBytes, StandardCharsets.UTF_8) == 'done'
    }

    private FnServletRequest<Object> newServletRequest(String requestUrl, Map<String, List<String>> headers = [:]) {
        def gatewayContext = new TestGatewayContext(requestUrl)
        def event = new ReadOnceInputEvent(
            InputStream.nullInputStream(),
            Headers.fromMultiHeaderMap(headers),
            'call',
            Instant.MAX
        )
        def response = new FnServletResponse<>(gatewayContext, conversionService)
        return new FnServletRequest<>(emptyByteBody(), event, response, gatewayContext, conversionService, null)
    }

    private static byte[] outputBytes(def nativeResponse) {
        def output = new ByteArrayOutputStream()
        nativeResponse.writeToOutput(output)
        return output.toByteArray()
    }

    private ByteBody emptyByteBody() {
        return InputStreamByteBody.create(InputStream.nullInputStream(), OptionalLong.of(0), ioExecutor, ByteArrayBufferFactory.INSTANCE)
    }

    static class TestGatewayContext implements HTTPGatewayContext {
        final String requestUrl
        final QueryParameters queryParameters = new QueryParametersImpl()
        final Map<String, List<String>> responseHeaders = [:].withDefault { [] }
        Integer statusCode

        TestGatewayContext(String requestUrl) {
            this.requestUrl = requestUrl
        }

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
            return 'GET'
        }

        @Override
        QueryParameters getQueryParameters() {
            return queryParameters
        }

        @Override
        void addResponseHeader(String key, String value) {
            responseHeaders.computeIfAbsent(key) { [] }.add(value)
        }

        @Override
        void setResponseHeader(String key, String v1, String... vs) {
            def values = [v1]
            if (vs != null) {
                values.addAll(vs)
            }
            responseHeaders.put(key, values)
        }

        @Override
        void setStatusCode(int code) {
            statusCode = code
        }
    }
}
