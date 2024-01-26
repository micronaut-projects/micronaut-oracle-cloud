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

import io.micrometer.core.instrument.Clock;
import io.micronaut.configuration.metrics.micrometer.ExportConfigurationProperties;
import io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.oraclecloud.core.TenancyIdProvider;
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudConfig;
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudMeterRegistry;
import io.micronaut.oraclecloud.monitoring.micrometer.OracleCloudRawMeterRegistry;
import io.micronaut.runtime.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
import java.util.Properties;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_EXPORT;

/**
 * The OracleCloudMeterRegistryFactory that will configure and create an oracle cloud monitoring meter registry.
 *
 * @author Pavol Gressa
 * @since 1.2
 */
@Factory
public class OracleCloudMeterRegistryFactory {

    public static final Logger LOG = LoggerFactory.getLogger(OracleCloudMeterRegistryFactory.class);
    public static final String ORACLECLOUD_METRICS_CONFIG = MICRONAUT_METRICS_EXPORT + "." + OracleCloudConfig.PREFIX;
    public static final String ORACLECLOUD_METRICS_ENABLED = ORACLECLOUD_METRICS_CONFIG + ".enabled";
    public static final String ORACLECLOUD_RAW_METRICS_ENABLED = ORACLECLOUD_METRICS_CONFIG + ".raw.enabled";

    private final TenancyIdProvider tenancyIdProvider;
    private final ApplicationConfiguration applicationConfiguration;

    /**
     * Creates OracleCloudMeterRegistryFactory.
     *
     * @param tenancyIdProvider             tenancy id provider
     * @param applicationConfiguration      micronaut application configuration
     */
    public OracleCloudMeterRegistryFactory(TenancyIdProvider tenancyIdProvider,
                                           ApplicationConfiguration applicationConfiguration) {
        this.tenancyIdProvider = tenancyIdProvider;
        this.applicationConfiguration = applicationConfiguration;
    }

    /**
     * Create an OracleCloudConfig bean if global metrics are enabled.
     * @param exportConfigurationProperties the exportConfigurationProperties configuration
     * @return A OracleCloudConfig
     */
    @Singleton
    @Requires(property = MeterRegistryFactory.MICRONAUT_METRICS_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
    OracleCloudConfig oracleCloudConfig(ExportConfigurationProperties exportConfigurationProperties) {
        exportConfigurationProperties.getExport().computeIfAbsent(OracleCloudConfig.PREFIX + ".applicationName",
            x -> applicationConfiguration.getName().orElse(null));

        exportConfigurationProperties.getExport().computeIfAbsent(OracleCloudConfig.PREFIX + ".compartmentId", x -> {
            LOG.info("Default compartmentId set to {}", tenancyIdProvider.getTenancyId());
            return tenancyIdProvider.getTenancyId();
        });

        Properties exportConfig = exportConfigurationProperties.getExport();
        return exportConfig::getProperty;
    }

    /**
     * Create an OracleCloudMeterRegistry bean if global metrics are enabled,
     * the oraclecloudmonitoring is enabled and raw metrics disabled.
     * Will be true by default when this configuration is included in project.
     *
     * @param oracleCloudConfig the OracleCloudConfig configuration
     * @param monitoringIngestionClient     the monitoring ingestion client
     * @return A OracleCloudMeterRegistry
     */
    @Singleton
    @Bean(preDestroy = "close")
    @Requires(property = MeterRegistryFactory.MICRONAUT_METRICS_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
    @Requires(property = OracleCloudMeterRegistryFactory.ORACLECLOUD_METRICS_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
    @Requires(property = OracleCloudMeterRegistryFactory.ORACLECLOUD_RAW_METRICS_ENABLED, notEquals = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
    OracleCloudMeterRegistry oracleCloudMeterRegistry(OracleCloudConfig oracleCloudConfig,
                                                      MonitoringIngestionClient monitoringIngestionClient) {
        return new OracleCloudMeterRegistry(oracleCloudConfig, Clock.SYSTEM, monitoringIngestionClient.getDelegate());
    }

    /**
     * Create a OracleCloudRawMeterRegistry bean if global metrics are enabled,
     * the oraclecloudmonitoring is enabled and raw metrics enabled.
     * Will be false by default when this configuration is included in project.
     *
     * @param oracleCloudConfig the OracleCloudConfig configuration
     * @param monitoringIngestionClient     the monitoring ingestion client
     * @return the registry
     */
    @Singleton
    @Bean(preDestroy = "close")
    @Requires(property = MeterRegistryFactory.MICRONAUT_METRICS_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
    @Requires(property = OracleCloudMeterRegistryFactory.ORACLECLOUD_RAW_METRICS_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    OracleCloudRawMeterRegistry oracleCloudRawMeterRegistry(OracleCloudConfig oracleCloudConfig, MonitoringIngestionClient monitoringIngestionClient) {
        return new OracleCloudRawMeterRegistry(oracleCloudConfig, Clock.SYSTEM, monitoringIngestionClient.getDelegate());
    }
}
