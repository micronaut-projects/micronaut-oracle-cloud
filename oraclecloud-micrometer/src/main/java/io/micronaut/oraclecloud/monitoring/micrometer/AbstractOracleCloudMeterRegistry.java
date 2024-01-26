/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.oraclecloud.monitoring.micrometer;

import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.MetricDataDetails;
import com.oracle.bmc.monitoring.model.PostMetricDataDetails;
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest;
import com.oracle.bmc.util.internal.StringUtils;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.lang.Nullable;
import io.micrometer.common.util.internal.logging.WarnThenDebugLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Common data and functions used both by {@link OracleCloudMeterRegistry} and {@link OracleCloudRawMeterRegistry}
 */
abstract class AbstractOracleCloudMeterRegistry extends StepMeterRegistry {
    protected final OracleCloudConfig oracleCloudConfig;
    private final Logger logger = LoggerFactory.getLogger(AbstractOracleCloudMeterRegistry.class);
    private final WarnThenDebugLogger warnThenDebugLogger = new WarnThenDebugLogger(OracleCloudMetricsNamingConvention.class);
    private final MonitoringClient monitoringClient;

    public AbstractOracleCloudMeterRegistry(OracleCloudConfig oracleCloudConfig, Clock clock, MonitoringClient monitoringClient, ThreadFactory threadFactory) {
        super(oracleCloudConfig, clock);
        this.monitoringClient = monitoringClient;
        this.oracleCloudConfig = oracleCloudConfig;
        config().namingConvention(new OracleCloudMetricsNamingConvention());
        config().commonTags("application", this.oracleCloudConfig.applicationName());
        start(threadFactory);
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    /**
     * Generates metric name.
     *
     * @param id meter id
     * @param suffix suffix
     * @return metric name
     */
    String getMetricName(Meter.Id id, @Nullable String suffix) {
        String name = suffix != null ? id.getName() + "_" + suffix : id.getName();
        return config().namingConvention().name(name, id.getType(), id.getBaseUnit());
    }

    /**
     * Transforms {@link Tag}s to dimensions. Tags that have empty key or value are ignored as
     * they are not consider valid dimensions.
     *
     * @param tags tags
     * @return map of tags
     */
    Map<String, String> toDimensions(List<Tag> tags) {
        Map<String, String> m = tags.stream().
            filter(this::isValidTag).
            collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        return m;
    }

    private boolean isValidTag(Tag tag) {
        if (StringUtils.isEmpty(tag.getKey()) || StringUtils.isEmpty(tag.getValue())) {
            warnThenDebugLogger.log("Tag " + tag.getKey() + " not published because tag key or value are empty.");
            return false;
        }
        return true;
    }

    protected abstract List<MetricDataDetails> getMetricData();

    @Override
    protected void publish() {
        for (List<MetricDataDetails> batch : MetricDataDetailsPartition.partition(getMetricData(), oracleCloudConfig.batchSize())) {
            final PostMetricDataDetails.Builder builder = PostMetricDataDetails.builder()
                .metricData(batch);
            if (oracleCloudConfig.batchAtomicity() != null) {
                builder.batchAtomicity(oracleCloudConfig.batchAtomicity());
            }
            try {
                monitoringClient.postMetricData(PostMetricDataRequest.builder()
                    .postMetricDataDetails(builder.build())
                    .build());
            } catch (Exception e) {
                logger.error("failed to post metrics to oracle cloud infrastructure monitoring: {}", e.getMessage(), e);
            }
        }
    }
}
