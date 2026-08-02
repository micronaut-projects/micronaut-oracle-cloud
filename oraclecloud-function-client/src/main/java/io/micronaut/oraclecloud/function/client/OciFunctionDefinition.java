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

import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.function.client.FunctionDefinition;
import org.jspecify.annotations.Nullable;

/**
 * OCI Functions definition configured for {@link io.micronaut.function.client.FunctionClient}.
 *
 * @since 6.0.0
 */
@EachProperty(OciFunctionDefinition.PREFIX)
public class OciFunctionDefinition implements FunctionDefinition {
    /**
     * Configuration prefix for OCI function definitions.
     */
    public static final String PREFIX = "oci.functions";

    private final String name;
    private String functionId;
    private InvokeFunctionRequest.FnIntent fnIntent;
    private InvokeFunctionRequest.FnInvokeType fnInvokeType;
    private @Nullable String opcRequestId;
    private @Nullable Boolean dryRun;

    /**
     * @param name The configured function name.
     */
    public OciFunctionDefinition(@Parameter String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * @return The OCID of the function to invoke.
     */
    public String getFunctionId() {
        return functionId;
    }

    /**
     * @param functionId The OCID of the function to invoke.
     */
    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }

    /**
     * @return The optional function intent header.
     */
    public InvokeFunctionRequest.FnIntent getFnIntent() {
        return fnIntent;
    }

    /**
     * @param fnIntent The optional function intent header.
     */
    public void setFnIntent(InvokeFunctionRequest.FnIntent fnIntent) {
        this.fnIntent = fnIntent;
    }

    /**
     * @return The optional invocation type header.
     */
    public InvokeFunctionRequest.FnInvokeType getFnInvokeType() {
        return fnInvokeType;
    }

    /**
     * @param fnInvokeType The optional invocation type header.
     */
    public void setFnInvokeType(InvokeFunctionRequest.FnInvokeType fnInvokeType) {
        this.fnInvokeType = fnInvokeType;
    }

    /**
     * @return The optional request id header.
     */
    public @Nullable String getOpcRequestId() {
        return opcRequestId;
    }

    /**
     * @param opcRequestId The optional request id header.
     */
    public void setOpcRequestId(@Nullable String opcRequestId) {
        this.opcRequestId = opcRequestId;
    }

    /**
     * @return Whether the invocation is a dry run.
     */
    public @Nullable Boolean getDryRun() {
        return dryRun;
    }

    /**
     * @param dryRun Whether the invocation is a dry run.
     */
    public void setDryRun(@Nullable Boolean dryRun) {
        this.dryRun = dryRun;
    }
}
