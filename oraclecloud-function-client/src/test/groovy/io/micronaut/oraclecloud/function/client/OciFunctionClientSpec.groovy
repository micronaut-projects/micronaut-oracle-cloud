/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.function.client

import com.oracle.bmc.functions.FunctionsInvokeAsyncClient
import com.oracle.bmc.functions.FunctionsInvokeClient
import com.oracle.bmc.functions.requests.InvokeFunctionRequest
import com.oracle.bmc.functions.responses.InvokeFunctionResponse
import com.oracle.bmc.responses.AsyncHandler
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.core.async.annotation.SingleResult
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.type.Argument
import io.micronaut.function.client.FunctionClient
import io.micronaut.function.client.FunctionDefinition
import io.micronaut.function.client.FunctionInvoker
import io.micronaut.function.client.FunctionInvokerChooser
import io.micronaut.function.client.exceptions.FunctionExecutionException
import io.micronaut.http.annotation.Body
import io.micronaut.json.JsonMapper
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import spock.lang.Specification

import jakarta.inject.Singleton
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

class OciFunctionClientSpec extends Specification {

    private static final String MOCK_CLIENTS_PROPERTY = 'spec.name'
    private static final String MOCK_CLIENTS_PROPERTY_VALUE = 'OciFunctionClientSpec'

    private static FunctionsInvokeClient mockSyncClient
    private static FunctionsInvokeAsyncClient mockAsyncClient

    void "configures OCI function definitions"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
            'oci.functions.books.function-id': 'ocid1.fnfunc.oc1..books',
            'oci.functions.books.fn-intent': 'Httprequest',
            'oci.functions.books.fn-invoke-type': 'Sync',
            'oci.functions.books.opc-request-id': 'request-1',
            'oci.functions.books.dry-run': true
        ])

        when:
        Collection<FunctionDefinition> definitions = applicationContext.getBeansOfType(FunctionDefinition)

        then:
        definitions.size() == 1

        when:
        OciFunctionDefinition definition = definitions.first() as OciFunctionDefinition

        then:
        definition.name == 'books'
        definition.functionId == 'ocid1.fnfunc.oc1..books'
        definition.fnIntent == InvokeFunctionRequest.FnIntent.Httprequest
        definition.fnInvokeType == InvokeFunctionRequest.FnInvokeType.Sync
        definition.opcRequestId == 'request-1'
        definition.dryRun

        cleanup:
        applicationContext.close()
    }

    void "invokes OCI function with FunctionClient"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        FunctionsInvokeAsyncClient asyncClient = Mock()
        mockSyncClient = syncClient
        mockAsyncClient = asyncClient
        InvokeFunctionRequest capturedRequest
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { InvokeFunctionRequest request ->
            capturedRequest = request
            response('{"title":"THE STAND"}')
        }
        ApplicationContext applicationContext = ApplicationContext.builder()
            .properties([
                (MOCK_CLIENTS_PROPERTY): MOCK_CLIENTS_PROPERTY_VALUE,
                'oci.functions.micronaut-function.function-id': 'ocid1.fnfunc.oc1..sync',
                'oci.functions.micronaut-function.fn-intent': 'Httprequest',
                'oci.functions.micronaut-function.fn-invoke-type': 'Sync'
            ])
            .start()

        when:
        BookClient client = applicationContext.getBean(BookClient)
        Map<String, Object> result = client.micronautFunction([title: 'The Stand'])

        then:
        result == [title: 'THE STAND']
        capturedRequest.functionId == 'ocid1.fnfunc.oc1..sync'
        capturedRequest.fnIntent == InvokeFunctionRequest.FnIntent.Httprequest
        capturedRequest.fnInvokeType == InvokeFunctionRequest.FnInvokeType.Sync
        new String(capturedRequest.invokeFunctionBody.readAllBytes(), StandardCharsets.UTF_8) == '{"title":"The Stand"}'

        cleanup:
        applicationContext.close()
        mockSyncClient = null
        mockAsyncClient = null
    }

    void "invokes OCI function reactively with FunctionClient"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        FunctionsInvokeAsyncClient asyncClient = Mock()
        mockSyncClient = syncClient
        mockAsyncClient = asyncClient
        InvokeFunctionRequest capturedRequest
        ApplicationContext applicationContext = ApplicationContext.builder()
            .properties([
                (MOCK_CLIENTS_PROPERTY): MOCK_CLIENTS_PROPERTY_VALUE,
                'oci.functions.reactive-function.function-id': 'ocid1.fnfunc.oc1..reactive',
                'oci.functions.reactive-function.fn-intent': 'Httprequest',
                'oci.functions.reactive-function.fn-invoke-type': 'Sync'
            ])
            .start()

        when:
        BookClient client = applicationContext.getBean(BookClient)
        Map<String, Object> result = Mono.from(client.reactiveFunction([title: 'The Stand'])).block()

        then:
        1 * asyncClient.invokeFunction(_ as InvokeFunctionRequest, _ as AsyncHandler) >> { InvokeFunctionRequest request, AsyncHandler handler ->
            capturedRequest = request
            handler.onSuccess(request, response('{"title":"THE STAND"}'))
            CompletableFuture.completedFuture(null)
        }
        0 * syncClient.invokeFunction(_ as InvokeFunctionRequest)
        result == [title: 'THE STAND']
        capturedRequest.functionId == 'ocid1.fnfunc.oc1..reactive'
        capturedRequest.fnIntent == InvokeFunctionRequest.FnIntent.Httprequest
        capturedRequest.fnInvokeType == InvokeFunctionRequest.FnInvokeType.Sync
        new String(capturedRequest.invokeFunctionBody.readAllBytes(), StandardCharsets.UTF_8) == '{"title":"The Stand"}'

        cleanup:
        applicationContext.close()
        mockSyncClient = null
        mockAsyncClient = null
    }

    void "invokes OCI function reactively"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        FunctionsInvokeAsyncClient asyncClient = Mock()
        InvokeFunctionRequest capturedRequest
        asyncClient.invokeFunction(_ as InvokeFunctionRequest, _ as AsyncHandler) >> { InvokeFunctionRequest request, AsyncHandler handler ->
            capturedRequest = request
            handler.onSuccess(request, response('{"title":"THE STAND"}'))
            CompletableFuture.completedFuture(null)
        }
        OciFunctionInvoker<Map<String, Object>, Publisher<Map<String, Object>>> invoker = new OciFunctionInvoker<>(
            syncClient,
            asyncClient,
            JsonMapper.createDefault(),
            ConversionService.SHARED
        )
        OciFunctionDefinition definition = new OciFunctionDefinition('books')
        definition.functionId = 'ocid1.fnfunc.oc1..reactive'

        when:
        Publisher<Map<String, Object>> publisher = invoker.invoke(
            definition,
            [title: 'The Stand'],
            Argument.of(Publisher, Argument.mapOf(String, Object))
        )
        Map<String, Object> result = Mono.from(publisher).block()

        then:
        result == [title: 'THE STAND']
        capturedRequest.functionId == 'ocid1.fnfunc.oc1..reactive'
        new String(capturedRequest.invokeFunctionBody.readAllBytes(), StandardCharsets.UTF_8) == '{"title":"The Stand"}'
    }

    void "rejects non OCI function definitions"() {
        given:
        OciFunctionInvoker invoker = new OciFunctionInvoker(
            null,
            null,
            JsonMapper.createDefault(),
            ConversionService.SHARED
        )

        when:
        invoker.invoke({ 'other' } as FunctionDefinition, [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        IllegalArgumentException e = thrown()
        e.message == 'Function definition must be an OciFunctionDefinition'
    }

    void "invokes OCI function without request body"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        InvokeFunctionRequest capturedRequest
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { InvokeFunctionRequest request ->
            capturedRequest = request
            response('{"title":"THE STAND"}')
        }
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        when:
        Map<String, Object> result = invoker.invoke(definition(), null, Argument.mapOf(String, Object))

        then:
        result == [title: 'THE STAND']
        capturedRequest.invokeFunctionBody == null
    }

    void "returns null for empty OCI function responses"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> response(responseBody as String)
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        expect:
        invoker.invoke(definition(), [title: 'The Stand'], Argument.mapOf(String, Object)) == null

        where:
        responseBody << [null, '']
    }

    void "closes response body for void output"() {
        given:
        CloseAwareInputStream inputStream = new CloseAwareInputStream('{"title":"THE STAND"}')
        FunctionsInvokeClient syncClient = Mock()
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> responseStream(inputStream)
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        when:
        Object result = invoker.invoke(definition(), [title: 'The Stand'], Argument.of(Void.TYPE))

        then:
        result == null
        inputStream.closed
    }

    void "wraps response body close errors"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> responseStream(new CloseFailureInputStream('{"title":"THE STAND"}'))
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        when:
        invoker.invoke(definition(), [title: 'The Stand'], Argument.of(Void.TYPE))

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Error executing OCI Function [books]: Error closing OCI Function response body: close failed'
    }

    void "wraps request encoding errors"() {
        given:
        JsonMapper jsonMapper = Mock()
        jsonMapper.writeValueAsBytes(_) >> { throw new IOException('encoding failed') }
        OciFunctionInvoker invoker = new OciFunctionInvoker(
            Mock(FunctionsInvokeClient),
            Mock(FunctionsInvokeAsyncClient),
            jsonMapper,
            ConversionService.SHARED
        )

        when:
        invoker.invoke(definition(), [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Error encoding OCI Function [books] request body: encoding failed'
    }

    void "wraps response decoding errors"() {
        given:
        JsonMapper jsonMapper = Mock()
        jsonMapper.writeValueAsBytes(_) >> '{}'.bytes
        jsonMapper.readValue(_ as byte[], _ as Argument) >> { throw new IOException('decoding failed') }
        FunctionsInvokeClient syncClient = Mock()
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> response('{"title":"THE STAND"}')
        OciFunctionInvoker invoker = new OciFunctionInvoker(
            syncClient,
            Mock(FunctionsInvokeAsyncClient),
            jsonMapper,
            ConversionService.SHARED
        )

        when:
        invoker.invoke(definition(), [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Error executing OCI Function [books]: Error decoding OCI Function [books] response body: decoding failed'
    }

    void "maps asynchronous OCI invocation errors"() {
        given:
        FunctionsInvokeAsyncClient asyncClient = Mock()
        asyncClient.invokeFunction(_ as InvokeFunctionRequest, _ as AsyncHandler) >> { InvokeFunctionRequest request, AsyncHandler handler ->
            handler.onError(request, new IllegalStateException('remote failure'))
            CompletableFuture.completedFuture(null)
        }
        OciFunctionInvoker invoker = invoker(Mock(FunctionsInvokeClient), asyncClient)

        when:
        Mono.from(invoker.invoke(definition(), [title: 'The Stand'], Argument.of(Publisher, Argument.mapOf(String, Object)))).block()

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Error executing OCI Function [books]: remote failure'
    }

    void "preserves asynchronous OCI response decoding errors"() {
        given:
        JsonMapper jsonMapper = Mock()
        jsonMapper.writeValueAsBytes(_) >> '{}'.bytes
        jsonMapper.readValue(_ as byte[], _ as Argument) >> { throw new IOException('decoding failed') }
        FunctionsInvokeAsyncClient asyncClient = Mock()
        asyncClient.invokeFunction(_ as InvokeFunctionRequest, _ as AsyncHandler) >> { InvokeFunctionRequest request, AsyncHandler handler ->
            handler.onSuccess(request, response('{"title":"THE STAND"}'))
            CompletableFuture.completedFuture(null)
        }
        OciFunctionInvoker invoker = new OciFunctionInvoker(
            Mock(FunctionsInvokeClient),
            asyncClient,
            jsonMapper,
            ConversionService.SHARED
        )

        when:
        Mono.from(invoker.invoke(definition(), [title: 'The Stand'], Argument.of(Publisher, Argument.mapOf(String, Object)))).block()

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Error decoding OCI Function [books] response body: decoding failed'
    }

    void "maps synchronous OCI invocation errors"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { throw new IllegalStateException('remote failure') }
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        when:
        invoker.invoke(definition(), [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Error executing OCI Function [books]: remote failure'
    }

    void "chooses invoker only for OCI function definitions"() {
        given:
        OciFunctionInvoker invoker = invoker(Mock(FunctionsInvokeClient), Mock(FunctionsInvokeAsyncClient))

        expect:
        invoker.choose(new OciFunctionDefinition('books')).isPresent()
        invoker.choose({ 'other' } as FunctionDefinition).isEmpty()
    }

    private static OciFunctionInvoker invoker(FunctionsInvokeClient syncClient, FunctionsInvokeAsyncClient asyncClient) {
        new OciFunctionInvoker(
            syncClient,
            asyncClient,
            JsonMapper.createDefault(),
            ConversionService.SHARED
        )
    }

    private static OciFunctionDefinition definition() {
        OciFunctionDefinition definition = new OciFunctionDefinition('books')
        definition.functionId = 'ocid1.fnfunc.oc1..books'
        definition
    }

    private static InvokeFunctionResponse response(String body) {
        responseStream(body == null ? null : new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))
    }

    private static InvokeFunctionResponse responseStream(InputStream body) {
        InvokeFunctionResponse.builder()
            .__httpStatusCode__(200)
            .inputStream(body)
            .build()
    }

    private static final class CloseAwareInputStream extends ByteArrayInputStream {
        boolean closed

        CloseAwareInputStream(String body) {
            super(body.getBytes(StandardCharsets.UTF_8))
        }

        @Override
        void close() throws IOException {
            closed = true
            super.close()
        }
    }

    private static final class CloseFailureInputStream extends ByteArrayInputStream {

        CloseFailureInputStream(String body) {
            super(body.getBytes(StandardCharsets.UTF_8))
        }

        @Override
        void close() throws IOException {
            throw new IOException('close failed')
        }
    }

    @FunctionClient
    static interface BookClient {
        Map<String, Object> micronautFunction(@Body Map<String, Object> book)

        @SingleResult
        Publisher<Map<String, Object>> reactiveFunction(@Body Map<String, Object> book)
    }

    @Factory
    @Requires(property = MOCK_CLIENTS_PROPERTY, value = MOCK_CLIENTS_PROPERTY_VALUE)
    static class MockFunctionClients {

        @Singleton
        @Replaces(FunctionsInvokeClient)
        FunctionsInvokeClient syncClient() {
            mockSyncClient
        }

        @Singleton
        @Replaces(FunctionsInvokeAsyncClient)
        FunctionsInvokeAsyncClient asyncClient() {
            mockAsyncClient
        }
    }
}
