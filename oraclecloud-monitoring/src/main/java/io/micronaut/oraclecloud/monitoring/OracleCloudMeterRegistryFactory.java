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

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.monitoring.MonitoringClient;
import io.micrometer.core.instrument.Clock;
import io.micronaut.configuration.metrics.micrometer.ExportConfigurationProperties;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Properties;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_EXPORT;

/**
 * The OracleCloudMeterRegistryFactory that will configure and create an oracle cloud monitoring meter registry.
 *
 * @author Pavol Gressa
 * @since 2.3
 */
@Requires(property = OracleCloudMeterRegistryFactory.ORACLECLOUD_METRICS_ENABLED, notEquals = StringUtils.FALSE)
@Factory
public class OracleCloudMeterRegistryFactory {

    public static final Logger LOG = LoggerFactory.getLogger(OracleCloudMeterRegistryFactory.class);
    public static final String ORACLECLOUD_METRICS_CONFIG = MICRONAUT_METRICS_EXPORT + ".oraclecloudmonitoring";
    public static final String ORACLECLOUD_METRICS_ENABLED = ORACLECLOUD_METRICS_CONFIG + ".enabled";

    private final AuthenticationDetailsProvider authenticationDetailsProvider;
    private final RegionProvider regionProvider;

    /**
     * Creates OracleCloudMeterRegistryFactory.
     *
     * @param authenticationDetailsProvider oci sdk authentication details provider
     * @param regionProvider                oci sdk region provider
     */
    public OracleCloudMeterRegistryFactory(AuthenticationDetailsProvider authenticationDetailsProvider, RegionProvider regionProvider) {
        this.authenticationDetailsProvider = authenticationDetailsProvider;
        this.regionProvider = regionProvider;
    }

    /**
     * Create a OracleCloudMeterRegistry bean if global metrics are enabled
     * and the oraclecloudmonitoring is enabled. Will be true by default when this
     * configuration is included in project.
     *
     * @param exportConfigurationProperties The export configuration
     * @return A OracleCloudMeterRegistry
     */
    @Singleton
    OracleCloudMeterRegistry oracleCloudMeterRegistry(ExportConfigurationProperties exportConfigurationProperties) {
        String ingestionEndpoint = String.format("https://telemetry-ingestion.%s.oraclecloud.com", regionProvider.getRegion().getRegionId());
        final MonitoringClient monitoringClient = MonitoringClient.builder().
                endpoint(ingestionEndpoint).
                build(authenticationDetailsProvider);

        exportConfigurationProperties.getExport().computeIfAbsent("oraclecloudmonitoring.compartmentId", x -> {
            if (LOG.isInfoEnabled()) {
                LOG.info("Default compartmentId set to " + authenticationDetailsProvider.getTenantId());
            }
            return authenticationDetailsProvider.getTenantId();
        });

        Properties exportConfig = exportConfigurationProperties.getExport();
        return new OracleCloudMeterRegistry(exportConfig::getProperty, Clock.SYSTEM, monitoringClient);
    }
}
