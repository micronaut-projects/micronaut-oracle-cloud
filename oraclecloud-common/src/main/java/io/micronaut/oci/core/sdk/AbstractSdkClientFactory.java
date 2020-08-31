/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oci.core.sdk;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.common.ClientBuilderBase;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.signing.RequestSignerFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.Objects;

/**
 * Abstract base factory for SDK objects.
 * @param <B> The builder type
 * @param <T> The client type
 */
public abstract class AbstractSdkClientFactory<B extends ClientBuilderBase<B, T>, T> {
    private final B builder;

    /**
     * Default constructor.
     * @param builder The builder
     * @param clientConfiguration The client config
     * @param clientConfigurator The client configurator (optional)
     * @param requestSignerFactory The request signer factory (optional)
     */
    protected AbstractSdkClientFactory(
            B builder,
            ClientConfiguration clientConfiguration,
            @Nullable ClientConfigurator clientConfigurator,
            @Nullable RequestSignerFactory requestSignerFactory) {
        this.builder = Objects.requireNonNull(builder, "Builder cannot be null");
        builder.configuration(Objects.requireNonNull(clientConfiguration, "Client configuration cannot be null"));
        if (clientConfigurator != null) {
            builder.clientConfigurator(clientConfigurator);
        }
        if (requestSignerFactory != null) {
            builder.requestSignerFactory(requestSignerFactory);
        }
    }

    /**
     * @return The builder
     */
    protected @NonNull B getBuilder() {
        return builder;
    }

    /**
     * Builds the client.
     * @param authenticationDetailsProvider The authentication details provider
     * @return The client to build
     */
    protected abstract @NonNull T build(@NonNull AbstractAuthenticationDetailsProvider authenticationDetailsProvider);
}
