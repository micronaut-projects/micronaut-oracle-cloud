/*
 * Copyright 2017-2023 original authors
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

import com.oracle.bmc.auth.DefaultServiceAccountTokenProvider;
import com.oracle.bmc.auth.ServiceAccountTokenSupplier;
import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.auth.internal.FederationClient;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeTenancyOnlyAuthenticationDetailsProvider;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.client.StandardClientProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/** Builder for OkeWorkloadIdentityAuthenticationDetailsProviderBuilder. */
class MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder extends OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder {
    private static OkeHttpClientConfiguration okeHttpClientConfiguration;

    /** The configuration for the circuit breaker. */
    private CircuitBreakerConfiguration circuitBreakerConfig;

    private ServiceAccountTokenSupplier serviceAccountTokenSupplier = new DefaultServiceAccountTokenProvider();

    public static void setOkeHttpClientConfiguration(OkeHttpClientConfiguration okeHttpClientConfiguration) {
        MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.okeHttpClientConfiguration = okeHttpClientConfiguration;
    }

    static OkeHttpClientConfiguration getOkeHttpClientConfiguration() {
        return okeHttpClientConfiguration;
    }

    /**
     Sets value for the circuit breaker configuration".
     @param circuitBreakerConfig the CircuitBreakerConfiguration
     */
     @Override
     public OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder circuitBreakerConfig(
        CircuitBreakerConfiguration circuitBreakerConfig) {
        this.circuitBreakerConfig = circuitBreakerConfig;
        return this;
    }

    @Override
    protected FederationClient createFederationClient(SessionKeySupplier sessionKeySupplier) {
        OkeTenancyOnlyAuthenticationDetailsProvider provider =
            new OkeTenancyOnlyAuthenticationDetailsProvider();

        ClientConfigurator configurator = builder -> {
            builder.property(StandardClientProperties.BUFFER_REQUEST, false);
        };

        List<ClientConfigurator> additionalConfigurators = new ArrayList<>();
        if (this.federationClientConfigurator != null) {
            additionalConfigurators.add(this.federationClientConfigurator);
        }
        additionalConfigurators.addAll(this.additionalFederationClientConfigurators);

        // create federation client
        return new MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient(
            sessionKeySupplier,
            serviceAccountTokenSupplier,
            provider,
            configurator,
            circuitBreakerConfig,
            additionalConfigurators
        );
    }

}
