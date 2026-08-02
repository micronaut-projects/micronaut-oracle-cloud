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
package io.micronaut.oraclecloud.function.client;

import com.oracle.bmc.functions.FunctionsInvokeAsyncClient;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.responses.InvokeFunctionResponse;
import com.oracle.bmc.responses.AsyncHandler;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.function.client.FunctionDefinition;
import io.micronaut.function.client.FunctionInvoker;
import io.micronaut.function.client.FunctionInvokerChooser;
import io.micronaut.function.client.exceptions.FunctionExecutionException;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Invokes OCI Functions through the OCI Functions Invoke SDK clients.
 *
 * @param <I> The input type
 * @param <O> The output type
 * @since 6.0.0
 */
@Singleton
@Internal
@Requires(beans = {FunctionsInvokeClient.class, FunctionsInvokeAsyncClient.class})
public final class OciFunctionInvoker<I, O> implements FunctionInvoker<I, O>, FunctionInvokerChooser {
    private final FunctionsInvokeClient syncClient;
    private final FunctionsInvokeAsyncClient asyncClient;
    private final JsonMapper jsonMapper;
    private final ConversionService conversionService;

    /**
     * @param syncClient The synchronous OCI Functions invoke client
     * @param asyncClient The asynchronous OCI Functions invoke client
     * @param jsonMapper The JSON mapper
     * @param conversionService The conversion service
     */
    OciFunctionInvoker(
        FunctionsInvokeClient syncClient,
        FunctionsInvokeAsyncClient asyncClient,
        JsonMapper jsonMapper,
        ConversionService conversionService
    ) {
        this.syncClient = syncClient;
        this.asyncClient = asyncClient;
        this.jsonMapper = jsonMapper;
        this.conversionService = conversionService;
    }

    @Override
    public @Nullable O invoke(FunctionDefinition definition, @Nullable I input, Argument<O> outputType) {
        if (!(definition instanceof OciFunctionDefinition ociFunctionDefinition)) {
            throw new IllegalArgumentException("Function definition must be an OciFunctionDefinition");
        }
        InvokeFunctionRequest request = createRequest(ociFunctionDefinition, input);
        Class<O> outputJavaType = outputType.getType();
        if (Publishers.isConvertibleToPublisher(outputJavaType)) {
            Argument<?> reactiveOutputType = outputType.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT);
            Mono<Object> response = Mono.create(sink ->
                asyncClient.invokeFunction(request, new AsyncHandler<>() {
                    @Override
                    public void onSuccess(InvokeFunctionRequest request, InvokeFunctionResponse response) {
                        try {
                            sink.success(decodeResponse(ociFunctionDefinition, response, reactiveOutputType));
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    }

                    @Override
                    public void onError(InvokeFunctionRequest request, Throwable error) {
                        sink.error(error);
                    }
                })
            ).onErrorMap(throwable -> throwable instanceof FunctionExecutionException ? throwable : new FunctionExecutionException(
                "Error executing OCI Function [" + definition.getName() + "]: " + throwable.getMessage(),
                throwable
            ));
            return conversionService.convert(response, outputType)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported reactive type: " + outputType));
        }
        InvokeFunctionResponse response;
        try {
            response = syncClient.invokeFunction(request);
        } catch (FunctionExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new FunctionExecutionException("Error executing OCI Function [" + definition.getName() + "]: " + e.getMessage(), e);
        }
        try {
            return (O) decodeResponse(ociFunctionDefinition, response, outputType);
        } catch (Exception e) {
            throw new FunctionExecutionException("Error executing OCI Function [" + definition.getName() + "]: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, O2> Optional<FunctionInvoker<I1, O2>> choose(FunctionDefinition definition) {
        if (definition instanceof OciFunctionDefinition) {
            return Optional.of((FunctionInvoker<I1, O2>) this);
        }
        return Optional.empty();
    }

    private InvokeFunctionRequest createRequest(OciFunctionDefinition definition, @Nullable I input) {
        String functionId = definition.getFunctionId();
        if (functionId == null || functionId.isBlank()) {
            throw new FunctionExecutionException(
                "Missing required configuration [" + OciFunctionDefinition.PREFIX + "." + definition.getName() + ".function-id]"
            );
        }
        return InvokeFunctionRequest.builder()
            .functionId(functionId)
            .fnIntent(definition.getFnIntent())
            .fnInvokeType(definition.getFnInvokeType())
            .opcRequestId(definition.getOpcRequestId())
            .isDryRun(definition.getDryRun())
            .invokeFunctionBody(encodeInput(definition, input))
            .build();
    }

    private @Nullable InputStream encodeInput(OciFunctionDefinition definition, @Nullable I input) {
        if (input == null) {
            return null;
        }
        try {
            return new ByteArrayInputStream(jsonMapper.writeValueAsBytes(input));
        } catch (IOException e) {
            throw new FunctionExecutionException("Error encoding OCI Function [" + definition.getName() + "] request body: " + e.getMessage(), e);
        }
    }

    private @Nullable Object decodeResponse(OciFunctionDefinition definition, InvokeFunctionResponse response, Argument<?> outputType) {
        if (outputType.isVoid()) {
            close(response.getInputStream());
            return null;
        }
        try (InputStream inputStream = response.getInputStream()) {
            if (inputStream == null) {
                return null;
            }
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                return null;
            }
            return jsonMapper.readValue(bytes, outputType);
        } catch (IOException e) {
            throw new FunctionExecutionException("Error decoding OCI Function [" + definition.getName() + "] response body: " + e.getMessage(), e);
        }
    }

    private void close(@Nullable InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new FunctionExecutionException("Error closing OCI Function response body: " + e.getMessage(), e);
            }
        }
    }
}
