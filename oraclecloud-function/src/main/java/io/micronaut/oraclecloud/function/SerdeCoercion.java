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
package io.micronaut.oraclecloud.function;

import com.fnproject.fn.api.InputCoercion;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.InvocationContext;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.OutputCoercion;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.runtime.coercion.ByteArrayCoercion;
import com.fnproject.fn.runtime.coercion.ContextCoercion;
import com.fnproject.fn.runtime.coercion.InputEventCoercion;
import com.fnproject.fn.runtime.coercion.OutputEventCoercion;
import com.fnproject.fn.runtime.coercion.StringCoercion;
import com.fnproject.fn.runtime.coercion.VoidCoercion;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

final class SerdeCoercion implements InputCoercion<Object>, OutputCoercion {
    // these coercions are higher priority than Jackson in FunctionRuntimeContext. Since
    // SerdeCoercion is placed at highest priority, we need to check these coercions in here, in
    // order to keep them functioning.
    private static final List<InputCoercion<?>> BUILTIN_INPUT_COERCIONS = List.of(new ContextCoercion(), new StringCoercion(), new ByteArrayCoercion(), new InputEventCoercion());
    private static final List<OutputCoercion> BUILTIN_OUTPUT_COERCIONS = List.of(new StringCoercion(), new ByteArrayCoercion(), new VoidCoercion(), new OutputEventCoercion());

    private final JsonMapper mapper;

    SerdeCoercion(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Object> tryCoerceParam(InvocationContext currentContext, int param, InputEvent input, MethodWrapper method) {
        for (InputCoercion<?> coercion : BUILTIN_INPUT_COERCIONS) {
            Optional<?> result = coercion.tryCoerceParam(currentContext, param, input, method);
            if (result.isPresent()) {
                return Optional.of(result.get());
            }
        }

        Type type = method.getTargetMethod().getGenericParameterTypes()[param];
        Argument<?> argument = Argument.of(type);

        return Optional.ofNullable(input.consumeBody(inputStream -> {
            try {
                return mapper.readValue(inputStream, argument);
            } catch (IOException e) {
                throw coercionFailed(type, e);
            }
        }));

    }

    private static RuntimeException coercionFailed(Type paramType, Throwable cause) {
        return new RuntimeException("Failed to coerce event to user function parameter type " + paramType, cause);
    }

    @Override
    public Optional<OutputEvent> wrapFunctionResult(InvocationContext ctx, MethodWrapper method, Object value) {
        for (OutputCoercion coercion : BUILTIN_OUTPUT_COERCIONS) {
            Optional<OutputEvent> result = coercion.wrapFunctionResult(ctx, method, value);
            if (result.isPresent()) {
                return result;
            }
        }

        try {
            return Optional.of(OutputEvent.fromBytes(mapper.writeValueAsBytes(value), OutputEvent.Status.Success,
                    "application/json"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to render response to JSON", e);
        }

    }
}
