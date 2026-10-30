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

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.functions.FunctionsInvokeAsyncClient
import com.oracle.bmc.functions.FunctionsInvokeClient
import com.oracle.bmc.functions.FunctionsManagementClient
import com.oracle.bmc.functions.model.Function
import com.oracle.bmc.functions.requests.GetFunctionRequest
import com.oracle.bmc.functions.requests.InvokeFunctionRequest
import com.oracle.bmc.functions.responses.GetFunctionResponse
import com.oracle.bmc.functions.responses.InvokeFunctionResponse
import com.oracle.bmc.responses.AsyncHandler
import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanProvider
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
    private static final String INVOKE_ENDPOINT = 'https://books.eu-frankfurt-1.functions.oci.oraclecloud.com'
    private static final String RESOLVED_INVOKE_ENDPOINT = 'https://resolved.eu-frankfurt-1.functions.oci.oraclecloud.com'

    private static OciFunctionInvokeClients mockInvokeClients
    private static FunctionsManagementClient mockManagementClient

    void "configures OCI function definitions"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
            'oci.functions.books.function-id': 'ocid1.fnfunc.oc1..books',
            'oci.functions.books.invoke-endpoint': INVOKE_ENDPOINT,
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
        definition.invokeEndpoint == INVOKE_ENDPOINT
        definition.fnIntent == InvokeFunctionRequest.FnIntent.Httprequest
        definition.fnInvokeType == InvokeFunctionRequest.FnInvokeType.Sync
        definition.opcRequestId == 'request-1'
        definition.dryRun

        cleanup:
        applicationContext.close()
    }

    void "invokes OCI function with FunctionClient at the configured invoke endpoint"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        OciFunctionInvokeClients invokeClients = Mock()
        FunctionsManagementClient managementClient = Mock()
        mockInvokeClients = invokeClients
        mockManagementClient = managementClient
        InvokeFunctionRequest capturedRequest
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { InvokeFunctionRequest request ->
            capturedRequest = request
            response('{"title":"THE STAND"}')
        }
        ApplicationContext applicationContext = ApplicationContext.builder()
            .properties([
                (MOCK_CLIENTS_PROPERTY): MOCK_CLIENTS_PROPERTY_VALUE,
                'oci.functions.micronaut-function.function-id': 'ocid1.fnfunc.oc1..sync',
                'oci.functions.micronaut-function.invoke-endpoint': INVOKE_ENDPOINT,
                'oci.functions.micronaut-function.fn-intent': 'Httprequest',
                'oci.functions.micronaut-function.fn-invoke-type': 'Sync'
            ])
            .start()

        when:
        BookClient client = applicationContext.getBean(BookClient)
        Map<String, Object> result = client.micronautFunction([title: 'The Stand'])

        then:
        1 * invokeClients.syncClient(INVOKE_ENDPOINT) >> syncClient
        0 * managementClient.getFunction(_)
        result == [title: 'THE STAND']
        capturedRequest.functionId == 'ocid1.fnfunc.oc1..sync'
        capturedRequest.fnIntent == InvokeFunctionRequest.FnIntent.Httprequest
        capturedRequest.fnInvokeType == InvokeFunctionRequest.FnInvokeType.Sync
        new String(capturedRequest.invokeFunctionBody.readAllBytes(), StandardCharsets.UTF_8) == '{"title":"The Stand"}'

        cleanup:
        applicationContext.close()
        mockInvokeClients = null
        mockManagementClient = null
    }

    void "invokes OCI function with FunctionClient at the invoke endpoint resolved from the function metadata"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        OciFunctionInvokeClients invokeClients = Mock()
        FunctionsManagementClient managementClient = Mock()
        mockInvokeClients = invokeClients
        mockManagementClient = managementClient
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { response('{"title":"THE STAND"}') }
        ApplicationContext applicationContext = ApplicationContext.builder()
            .properties([
                (MOCK_CLIENTS_PROPERTY): MOCK_CLIENTS_PROPERTY_VALUE,
                'oci.functions.micronaut-function.function-id': 'ocid1.fnfunc.oc1..sync'
            ])
            .start()

        when:
        BookClient client = applicationContext.getBean(BookClient)
        Map<String, Object> first = client.micronautFunction([title: 'The Stand'])
        Map<String, Object> second = client.micronautFunction([title: 'The Stand'])

        then:
        1 * managementClient.getFunction(_ as GetFunctionRequest) >> { GetFunctionRequest request ->
            assert request.functionId == 'ocid1.fnfunc.oc1..sync'
            functionResponse(RESOLVED_INVOKE_ENDPOINT)
        }
        2 * invokeClients.syncClient(RESOLVED_INVOKE_ENDPOINT) >> syncClient
        first == [title: 'THE STAND']
        second == [title: 'THE STAND']

        cleanup:
        applicationContext.close()
        mockInvokeClients = null
        mockManagementClient = null
    }

    void "invokes OCI function reactively with FunctionClient"() {
        given:
        FunctionsInvokeAsyncClient asyncClient = Mock()
        OciFunctionInvokeClients invokeClients = Mock()
        FunctionsManagementClient managementClient = Mock()
        mockInvokeClients = invokeClients
        mockManagementClient = managementClient
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
        1 * managementClient.getFunction(_ as GetFunctionRequest) >> functionResponse(RESOLVED_INVOKE_ENDPOINT)
        1 * invokeClients.asyncClient(RESOLVED_INVOKE_ENDPOINT) >> asyncClient
        0 * invokeClients.syncClient(_)
        1 * asyncClient.invokeFunction(_ as InvokeFunctionRequest, _ as AsyncHandler) >> { InvokeFunctionRequest request, AsyncHandler handler ->
            capturedRequest = request
            handler.onSuccess(request, response('{"title":"THE STAND"}'))
            CompletableFuture.completedFuture(null)
        }
        result == [title: 'THE STAND']
        capturedRequest.functionId == 'ocid1.fnfunc.oc1..reactive'
        capturedRequest.fnIntent == InvokeFunctionRequest.FnIntent.Httprequest
        capturedRequest.fnInvokeType == InvokeFunctionRequest.FnInvokeType.Sync
        new String(capturedRequest.invokeFunctionBody.readAllBytes(), StandardCharsets.UTF_8) == '{"title":"The Stand"}'

        cleanup:
        applicationContext.close()
        mockInvokeClients = null
        mockManagementClient = null
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
            invokeClients(syncClient, asyncClient),
            managementClientProvider(Mock(FunctionsManagementClient)),
            JsonMapper.createDefault(),
            ConversionService.SHARED
        )
        OciFunctionDefinition definition = new OciFunctionDefinition('books')
        definition.functionId = 'ocid1.fnfunc.oc1..reactive'
        definition.invokeEndpoint = INVOKE_ENDPOINT

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

    void "rejects missing OCI function id before invocation"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        OciFunctionDefinition definition = new OciFunctionDefinition('books')
        definition.functionId = functionId
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        when:
        invoker.invoke(definition, [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Missing required configuration [oci.functions.books.function-id]'
        0 * syncClient.invokeFunction(_ as InvokeFunctionRequest)

        where:
        functionId << [null, '', '  ']
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

    void "returns null for detached OCI function responses without function output"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        InvokeFunctionRequest capturedRequest
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { InvokeFunctionRequest request ->
            capturedRequest = request
            response('', 202)
        }
        OciFunctionDefinition definition = definition()
        definition.fnInvokeType = InvokeFunctionRequest.FnInvokeType.Detached
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        expect:
        invoker.invoke(definition, [title: 'The Stand'], Argument.mapOf(String, Object)) == null
        capturedRequest.fnInvokeType == InvokeFunctionRequest.FnInvokeType.Detached
    }

    void "requires JSON response bodies for non empty output"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> response('plain text')
        OciFunctionInvoker invoker = invoker(syncClient, Mock(FunctionsInvokeAsyncClient))

        when:
        invoker.invoke(definition(), [title: 'The Stand'], Argument.STRING)

        then:
        FunctionExecutionException e = thrown()
        e.message.startsWith('Error executing OCI Function [books]: Error decoding OCI Function [books] response body:')
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
            invokeClients(Mock(FunctionsInvokeClient), Mock(FunctionsInvokeAsyncClient)),
            managementClientProvider(Mock(FunctionsManagementClient)),
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
            invokeClients(syncClient, Mock(FunctionsInvokeAsyncClient)),
            managementClientProvider(Mock(FunctionsManagementClient)),
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
            invokeClients(Mock(FunctionsInvokeClient), asyncClient),
            managementClientProvider(Mock(FunctionsManagementClient)),
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

    void "invokes OCI function at the configured invoke endpoint without resolving the function metadata"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        FunctionsManagementClient managementClient = Mock()
        RecordingInvokeClients invokeClients = new RecordingInvokeClients(syncClient, Mock(FunctionsInvokeAsyncClient))
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> response('{"title":"THE STAND"}')
        OciFunctionInvoker invoker = invoker(invokeClients, managementClient)
        OciFunctionDefinition definition = definition()
        definition.invokeEndpoint = 'https://explicit.eu-frankfurt-1.functions.oci.oraclecloud.com'

        when:
        Map<String, Object> result = invoker.invoke(definition, [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        result == [title: 'THE STAND']
        invokeClients.syncEndpoints == ['https://explicit.eu-frankfurt-1.functions.oci.oraclecloud.com']
        0 * managementClient.getFunction(_)
    }

    void "resolves the invoke endpoint from the function metadata once per function"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        FunctionsInvokeAsyncClient asyncClient = Mock()
        FunctionsManagementClient managementClient = Mock()
        RecordingInvokeClients invokeClients = new RecordingInvokeClients(syncClient, asyncClient)
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { response('{"title":"THE STAND"}') }
        asyncClient.invokeFunction(_ as InvokeFunctionRequest, _ as AsyncHandler) >> { InvokeFunctionRequest request, AsyncHandler handler ->
            handler.onSuccess(request, response('{"title":"THE STAND"}'))
            CompletableFuture.completedFuture(null)
        }
        OciFunctionInvoker invoker = invoker(invokeClients, managementClient)
        OciFunctionDefinition definition = definitionWithoutInvokeEndpoint()
        definition.invokeEndpoint = blankInvokeEndpoint

        when:
        Map<String, Object> first = invoker.invoke(definition, [title: 'The Stand'], Argument.mapOf(String, Object))
        Map<String, Object> second = invoker.invoke(definition, [title: 'The Stand'], Argument.mapOf(String, Object))
        Map<String, Object> third = Mono.from(invoker.invoke(definition, [title: 'The Stand'], Argument.of(Publisher, Argument.mapOf(String, Object)))).block()

        then:
        1 * managementClient.getFunction(_ as GetFunctionRequest) >> { GetFunctionRequest request ->
            assert request.functionId == 'ocid1.fnfunc.oc1..books'
            functionResponse(RESOLVED_INVOKE_ENDPOINT)
        }
        first == [title: 'THE STAND']
        second == [title: 'THE STAND']
        third == [title: 'THE STAND']
        invokeClients.syncEndpoints == [RESOLVED_INVOKE_ENDPOINT, RESOLVED_INVOKE_ENDPOINT]
        invokeClients.asyncEndpoints == [RESOLVED_INVOKE_ENDPOINT]

        where:
        blankInvokeEndpoint << [null, '', '  ']
    }

    void "does not cache failed invoke endpoint lookups"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        FunctionsManagementClient managementClient = Mock()
        RecordingInvokeClients invokeClients = new RecordingInvokeClients(syncClient, Mock(FunctionsInvokeAsyncClient))
        syncClient.invokeFunction(_ as InvokeFunctionRequest) >> { response('{"title":"THE STAND"}') }
        OciFunctionInvoker invoker = invoker(invokeClients, managementClient)

        when:
        invoker.invoke(definitionWithoutInvokeEndpoint(), [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        1 * managementClient.getFunction(_ as GetFunctionRequest) >> { throw new IllegalStateException('lookup failed') }
        FunctionExecutionException e = thrown()
        e.message == 'Error resolving the invoke endpoint of OCI Function [books]: lookup failed'
        e.cause instanceof IllegalStateException
        invokeClients.syncEndpoints.isEmpty()

        when:
        Map<String, Object> result = invoker.invoke(definitionWithoutInvokeEndpoint(), [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        1 * managementClient.getFunction(_ as GetFunctionRequest) >> functionResponse(RESOLVED_INVOKE_ENDPOINT)
        result == [title: 'THE STAND']
        invokeClients.syncEndpoints == [RESOLVED_INVOKE_ENDPOINT]
    }

    void "rejects function metadata without an invoke endpoint"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        FunctionsManagementClient managementClient = Mock()
        managementClient.getFunction(_ as GetFunctionRequest) >> functionResponse(invokeEndpoint as String)
        OciFunctionInvoker invoker = invoker(new RecordingInvokeClients(syncClient, Mock(FunctionsInvokeAsyncClient)), managementClient)

        when:
        invoker.invoke(definitionWithoutInvokeEndpoint(), [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        FunctionExecutionException e = thrown()
        e.message == 'OCI Function [books] with id [ocid1.fnfunc.oc1..books] has no invoke endpoint. Configure [oci.functions.books.invoke-endpoint]'
        0 * syncClient.invokeFunction(_)

        where:
        invokeEndpoint << [null, '']
    }

    void "requires a FunctionsManagementClient to resolve the invoke endpoint"() {
        given:
        FunctionsInvokeClient syncClient = Mock()
        BeanProvider<FunctionsManagementClient> managementClientProvider = [
            isPresent: { false },
            get: { throw new IllegalStateException('no bean') }
        ] as BeanProvider<FunctionsManagementClient>
        OciFunctionInvoker invoker = new OciFunctionInvoker(
            new RecordingInvokeClients(syncClient, Mock(FunctionsInvokeAsyncClient)),
            managementClientProvider,
            JsonMapper.createDefault(),
            ConversionService.SHARED
        )

        when:
        invoker.invoke(definitionWithoutInvokeEndpoint(), [title: 'The Stand'], Argument.mapOf(String, Object))

        then:
        FunctionExecutionException e = thrown()
        e.message == 'Cannot resolve the invoke endpoint of OCI Function [books]: no FunctionsManagementClient bean is available. Configure [oci.functions.books.invoke-endpoint]'
        0 * syncClient.invokeFunction(_)
    }

    void "builds one invoke client per invoke endpoint"() {
        given:
        DefaultOciFunctionInvokeClients invokeClients = new DefaultOciFunctionInvokeClients(
            FunctionsInvokeClient.builder(),
            FunctionsInvokeAsyncClient.builder(),
            new TestAuthenticationDetailsProvider()
        )
        String otherInvokeEndpoint = 'https://other.eu-frankfurt-1.functions.oci.oraclecloud.com'

        when:
        FunctionsInvokeClient syncClient = invokeClients.syncClient(INVOKE_ENDPOINT)
        FunctionsInvokeClient otherSyncClient = invokeClients.syncClient(otherInvokeEndpoint)
        FunctionsInvokeAsyncClient asyncClient = invokeClients.asyncClient(INVOKE_ENDPOINT)
        FunctionsInvokeAsyncClient otherAsyncClient = invokeClients.asyncClient(otherInvokeEndpoint)

        then:
        syncClient.endpoint == INVOKE_ENDPOINT
        otherSyncClient.endpoint == otherInvokeEndpoint
        asyncClient.endpoint == INVOKE_ENDPOINT
        otherAsyncClient.endpoint == otherInvokeEndpoint
        !syncClient.is(otherSyncClient)
        !asyncClient.is(otherAsyncClient)
        invokeClients.syncClient(INVOKE_ENDPOINT).is(syncClient)
        invokeClients.asyncClient(INVOKE_ENDPOINT).is(asyncClient)

        when:
        invokeClients.close()

        then:
        !invokeClients.syncClient(INVOKE_ENDPOINT).is(syncClient)
        !invokeClients.asyncClient(INVOKE_ENDPOINT).is(asyncClient)

        cleanup:
        invokeClients.close()
    }

    private static OciFunctionInvoker invoker(FunctionsInvokeClient syncClient, FunctionsInvokeAsyncClient asyncClient) {
        invoker(invokeClients(syncClient, asyncClient), null)
    }

    private static OciFunctionInvoker invoker(OciFunctionInvokeClients invokeClients, FunctionsManagementClient managementClient) {
        new OciFunctionInvoker(
            invokeClients,
            managementClientProvider(managementClient),
            JsonMapper.createDefault(),
            ConversionService.SHARED
        )
    }

    private static OciFunctionInvokeClients invokeClients(FunctionsInvokeClient syncClient, FunctionsInvokeAsyncClient asyncClient) {
        new RecordingInvokeClients(syncClient, asyncClient)
    }

    private static BeanProvider<FunctionsManagementClient> managementClientProvider(FunctionsManagementClient managementClient) {
        [
            isPresent: { managementClient != null },
            get: { managementClient }
        ] as BeanProvider<FunctionsManagementClient>
    }

    private static GetFunctionResponse functionResponse(String invokeEndpoint) {
        GetFunctionResponse.builder()
            .__httpStatusCode__(200)
            .function(Function.builder().id('ocid1.fnfunc.oc1..books').invokeEndpoint(invokeEndpoint).build())
            .build()
    }

    private static OciFunctionDefinition definition() {
        OciFunctionDefinition definition = definitionWithoutInvokeEndpoint()
        definition.invokeEndpoint = INVOKE_ENDPOINT
        definition
    }

    private static OciFunctionDefinition definitionWithoutInvokeEndpoint() {
        OciFunctionDefinition definition = new OciFunctionDefinition('books')
        definition.functionId = 'ocid1.fnfunc.oc1..books'
        definition
    }

    private static InvokeFunctionResponse response(String body) {
        response(body, 200)
    }

    private static InvokeFunctionResponse response(String body, int statusCode) {
        responseStream(body == null ? null : new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), statusCode)
    }

    private static InvokeFunctionResponse responseStream(InputStream body) {
        responseStream(body, 200)
    }

    private static InvokeFunctionResponse responseStream(InputStream body, int statusCode) {
        InvokeFunctionResponse.builder()
            .__httpStatusCode__(statusCode)
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

    private static final class RecordingInvokeClients implements OciFunctionInvokeClients {
        final FunctionsInvokeClient syncClient
        final FunctionsInvokeAsyncClient asyncClient
        final List<String> syncEndpoints = []
        final List<String> asyncEndpoints = []

        RecordingInvokeClients(FunctionsInvokeClient syncClient, FunctionsInvokeAsyncClient asyncClient) {
            this.syncClient = syncClient
            this.asyncClient = asyncClient
        }

        @Override
        FunctionsInvokeClient syncClient(String invokeEndpoint) {
            syncEndpoints << invokeEndpoint
            syncClient
        }

        @Override
        FunctionsInvokeAsyncClient asyncClient(String invokeEndpoint) {
            asyncEndpoints << invokeEndpoint
            asyncClient
        }
    }

    private static final class TestAuthenticationDetailsProvider implements AuthenticationDetailsProvider {
        private static final String DUMMY_PEM_KEY = '''-----BEGIN RSA PRIVATE KEY-----
MIIBOAIBAAJAUczpZlq0T4QOr4F1RAg/lp0CJLn56ldrmis7bDQ1+XiC3/j7DzhP
oLCd2PWHU/jniJdWAw6wESix/nb0xs/EiQIDAQABAkAqmNqyQmnDPrGnE3NNij4S
4JBNL8vFDOEr13eKUWYKEvAAYEnscgyWQvGb7yvAQ5z/YBYatnAjakHRDO5kXtAB
AiEAoQm2tcP3IiBm8BxstWKJlJ3xYA1euqLFdPnAaPQ5L6ECIQCCCYE0CSeLxgWw
YqJyStqFbAzlUO1yarWIL3L61IeL6QIgenKQYxVmzKQmoVx7rFAInOCbsJV5+h/a
VF+zVhqdgQECIEZZ3gzI5xw3hdxngHtVA+QrEM7/eXbtREjpYstRMAQBAiAy3g7Q
ikw16ABtUnL1IVcwxBPZpSowDd5G3bcJyt+NSQ==
-----END RSA PRIVATE KEY-----'''

        @Override
        String getKeyId() {
            'ocid1.tenancy.oc1..test/ocid1.user.oc1..test/00:00'
        }

        @Override
        InputStream getPrivateKey() {
            new ByteArrayInputStream(DUMMY_PEM_KEY.getBytes(StandardCharsets.UTF_8))
        }

        @Override
        String getPassPhrase() {
            null
        }

        @Override
        char[] getPassphraseCharacters() {
            new char[0]
        }

        @Override
        String getFingerprint() {
            '00:00'
        }

        @Override
        String getTenantId() {
            'ocid1.tenancy.oc1..test'
        }

        @Override
        String getUserId() {
            'ocid1.user.oc1..test'
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
        @Replaces(OciFunctionInvokeClients)
        OciFunctionInvokeClients invokeClients() {
            mockInvokeClients
        }

        @Singleton
        @Replaces(FunctionsManagementClient)
        FunctionsManagementClient managementClient() {
            mockManagementClient
        }
    }
}
