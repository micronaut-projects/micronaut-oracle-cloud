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
package io.micronaut.oraclecloud.monitoring;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.signing.RequestSignerFactory;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest;
import com.oracle.bmc.monitoring.responses.PostMetricDataResponse;
import io.micronaut.core.annotation.Nullable;

import jakarta.inject.Singleton;
import java.util.Objects;

/**
 * Oracle SDK provides {@link MonitoringClient} that is constructed with default {@code https://telemetry.<region>.oraclecloud.com} endpoint.
 * For sending metrics to the Oracle Cloud Monitoring service the {@link MonitoringClient#postMetricData(PostMetricDataRequest)} is used
 * but the endpoint must be configured to {@code https://telemetry-ingestion.<region>.oraclecloud.com}. This bean encapsulates creation
 * and configuration of the {@link MonitoringClient} to use {@link MonitoringClient#postMetricData(PostMetricDataRequest)}
 * so the {@link MonitoringClient} concerns are separated into two singleton beans.
 *
 * @author Pavol Gressa
 * @since 1.2
 */
@Singleton
public class MonitoringIngestionClient {

    private final ClientConfiguration clientConfiguration;
    private final ClientConfigurator clientConfigurator;
    private final RequestSignerFactory requestSignerFactory;
    private final RegionProvider regionProvider;
    private final AbstractAuthenticationDetailsProvider authenticationDetailsProvider;

    private MonitoringClient delegate;

    /**
     * Creates {@link MonitoringIngestionClient}.
     *
     * @param clientConfiguration           client configuration
     * @param clientConfigurator            client configurator
     * @param requestSignerFactory          request signer factory
     * @param regionProvider                region provider
     * @param authenticationDetailsProvider authentication details provider
     */
    public MonitoringIngestionClient(ClientConfiguration clientConfiguration,
                                     @Nullable ClientConfigurator clientConfigurator,
                                     @Nullable RequestSignerFactory requestSignerFactory,
                                     RegionProvider regionProvider,
                                     AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
        this.clientConfiguration = clientConfiguration;
        this.clientConfigurator = clientConfigurator;
        this.requestSignerFactory = requestSignerFactory;
        this.regionProvider = regionProvider;
        this.authenticationDetailsProvider = authenticationDetailsProvider;
    }

    /**
     * Gets the {@link MonitoringClient} delegate.
     *
     * @return monitoring client
     */
    public MonitoringClient getDelegate() {
        if (delegate == null) {
            synchronized (MonitoringIngestionClient.class) {
                if (delegate == null) {
                    String ingestionEndpoint = String.format("https://telemetry-ingestion.%s.oraclecloud.com",
                            regionProvider.getRegion().getRegionId());
                    MonitoringClient.Builder builder = MonitoringClient.builder().
                            endpoint(ingestionEndpoint);

                    builder.configuration(Objects.requireNonNull(clientConfiguration, "Client configuration cannot be null"));
                    if (clientConfigurator != null) {
                        builder.clientConfigurator(clientConfigurator);
                    }
                    if (requestSignerFactory != null) {
                        builder.requestSignerFactory(requestSignerFactory);
                    }

                    delegate = builder.build(authenticationDetailsProvider);
                }
            }
        }
        return delegate;
    }

    /**
     * Post {@link PostMetricDataRequest}.
     *
     * @param request request
     * @return response
     */
    public PostMetricDataResponse postMetricData(PostMetricDataRequest request) {
        return getDelegate().postMetricData(request);
    }
}
