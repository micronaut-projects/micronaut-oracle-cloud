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
package io.micronaut.oci.objectstorage;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.signing.RequestSignerFactory;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.oci.core.sdk.AbstractSdkClientFactory;

import javax.inject.Singleton;

/**
 * Factory for creating the object storage client.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Factory
@Requires(classes = ObjectStorageClient.class)
public class ObjectStorageClientFactory extends AbstractSdkClientFactory<ObjectStorageClient.Builder, ObjectStorageClient> {

    private final ObjectStorageClient.Builder builder;

    /**
     * Default constructor.
     * @param clientConfiguration The client configuration
     * @param clientConfigurator The client configurator (optional)
     * @param requestSignerFactory THe request signer factory (optional)
     */
    protected ObjectStorageClientFactory(
            ClientConfiguration clientConfiguration,
            @Nullable ClientConfigurator clientConfigurator,
            @Nullable RequestSignerFactory requestSignerFactory) {
        super(ObjectStorageClient.builder(), clientConfiguration, clientConfigurator, requestSignerFactory);
        builder = super.getBuilder();
    }

    @Override
    @Singleton
    @Requires(classes = ObjectStorageClient.class)
    protected ObjectStorageClient.Builder getBuilder() {
        return super.getBuilder();
    }

    @Singleton
    @Requires(classes = ObjectStorageClient.class)
    @Override
    protected ObjectStorageClient build(@NonNull AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
        return builder.build(authenticationDetailsProvider);
    }
}
