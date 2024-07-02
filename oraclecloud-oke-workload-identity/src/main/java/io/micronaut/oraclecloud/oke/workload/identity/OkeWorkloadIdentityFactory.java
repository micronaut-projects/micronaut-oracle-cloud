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
package io.micronaut.oraclecloud.oke.workload.identity;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Sets up core beans for integration with Oracle cloud clients. The following beans are exposed:
 *
 * <ul>
 *     <li>{@code AuthenticationDetailsProvider}</li>
 *     <li>{@code ClientConfiguration.ClientConfigurationBuilder}</li>
 *     <li>{@code ClientConfiguration}</li>
 * </ul>
 *
 * @see ConfigFileAuthenticationDetailsProvider
 * @see ResourcePrincipalAuthenticationDetailsProvider
 * @see SimpleAuthenticationDetailsProvider
 */
@Factory
@BootstrapContextCompatible
public class OkeWorkloadIdentityFactory {
    @Inject
    @Nullable
    SessionKeySupplier sessionKeySupplier;

    /**
     * Configures a {@link OkeWorkloadIdentityAuthenticationDetailsProvider} if no other {@link AuthenticationDetailsProvider} is present and
     * the specified by the user with {@code oci.config.use-instance-principal}.
     *
     * @param okeWorkloadIdentityConfiguration The configuration
     * @return The {@link OkeWorkloadIdentityAuthenticationDetailsProvider}.
     * @see OkeWorkloadIdentityAuthenticationDetailsProvider
     */
    @Singleton
    @Requires(beans = OkeWorkloadIdentityConfiguration.class)
    @Primary
    @BootstrapContextCompatible
    protected OkeWorkloadIdentityAuthenticationDetailsProvider okeWorkloadIdentityAuthenticationDetailsProvider(OkeWorkloadIdentityConfiguration okeWorkloadIdentityConfiguration) {
        OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder builder = okeWorkloadIdentityConfiguration.getBuilder();
        if (sessionKeySupplier != null) {
            builder.sessionKeySupplier(sessionKeySupplier);
        }
        return builder.build();
    }

}
