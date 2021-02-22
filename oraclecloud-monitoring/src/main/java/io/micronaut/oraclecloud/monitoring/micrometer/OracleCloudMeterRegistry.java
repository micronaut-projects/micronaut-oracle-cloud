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
package io.micronaut.oraclecloud.monitoring.micrometer;

import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.monitoring.model.Datapoint;
import com.oracle.bmc.monitoring.model.MetricDataDetails;
import com.oracle.bmc.monitoring.model.PostMetricDataDetails;
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.core.lang.Nullable;
import io.micrometer.core.util.internal.logging.WarnThenDebugLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

/**
 * {@link StepMeterRegistry} for Oracle Cloud Monitoring.
 *
 * @author Pavol Gressa
 * @since 1.2
 */
public class OracleCloudMeterRegistry extends StepMeterRegistry {

    private final WarnThenDebugLogger warnThenDebugLogger = new WarnThenDebugLogger(OracleCloudMetricsNamingConvention.class);
    private final Logger logger = LoggerFactory.getLogger(OracleCloudMeterRegistry.class);

    private final MonitoringClient monitoringClient;
    private final OracleCloudConfig oracleCloudConfig;

    public OracleCloudMeterRegistry(OracleCloudConfig oracleCloudConfig,
                                    Clock clock,
                                    MonitoringClient monitoringClient) {
        this(oracleCloudConfig, clock, monitoringClient, new NamedThreadFactory("oraclecloud-metrics-publisher"));
    }

    public OracleCloudMeterRegistry(OracleCloudConfig oracleCloudConfig,
                                    Clock clock,
                                    MonitoringClient monitoringClient,
                                    ThreadFactory threadFactory) {
        super(oracleCloudConfig, clock);
        this.monitoringClient = monitoringClient;
        this.oracleCloudConfig = oracleCloudConfig;

        config().namingConvention(new OracleCloudMetricsNamingConvention());
        config().commonTags("application", oracleCloudConfig.applicationName());
        start(threadFactory);
    }

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
            } catch (Throwable e) {
                logger.error("failed to post metrics in oraclecloud monitor: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    /**
     * @return list of all {@link Meter} data transformed into {@link MetricDataDetails}
     */
    List<MetricDataDetails> getMetricData() {
        return getMeters().stream().flatMap(meter -> meter.match(
                this::trackGauge,
                this::trackCounter,
                this::trackTimer,
                this::trackDistributionSummary,
                this::trackLongTaskTimer,
                this::trackTimeGauge,
                this::trackFunctionCounter,
                this::trackFunctionTimer,
                this::trackMeter)
        ).collect(Collectors.toList());
    }

    /**
     * @param gauge gauge meter
     * @return {@link MetricDataDetails} stream with gauge values or null if gauge value is NaN
     */
    Stream<MetricDataDetails> trackGauge(Gauge gauge) {
        MetricDataDetails metricDataDetails = metricDataDetails(gauge.getId(), "value", gauge.value());
        if (metricDataDetails == null) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails);
    }

    /**
     * @param counter counter meter
     * @return {@link MetricDataDetails} stream with counter values
     */
    Stream<MetricDataDetails> trackCounter(Counter counter) {
        return Stream.of(metricDataDetails(counter.getId(), "count", counter.count()));
    }

    /**
     * @param timer timer meter
     * @return {@link MetricDataDetails} with Timer values
     */
    Stream<MetricDataDetails> trackTimer(Timer timer) {
        Stream.Builder<MetricDataDetails> metrics = Stream.builder();
        metrics.add(metricDataDetails(timer.getId(), "sum", timer.totalTime(getBaseTimeUnit())));
        long count = timer.count();
        metrics.add(metricDataDetails(timer.getId(), "count", count));
        if (count > 0) {
            metrics.add(metricDataDetails(timer.getId(), "avg", timer.mean(getBaseTimeUnit())));
            metrics.add(metricDataDetails(timer.getId(), "max", timer.max(getBaseTimeUnit())));
        }
        return metrics.build();
    }

    /**
     * @param summary distribution summary meter
     * @return {@link MetricDataDetails} stream with DistributionSummary values
     */
    Stream<MetricDataDetails> trackDistributionSummary(DistributionSummary summary) {
        Stream.Builder<MetricDataDetails> metrics = Stream.builder();
        metrics.add(metricDataDetails(summary.getId(), "sum", summary.totalAmount()));
        long count = summary.count();
        metrics.add(metricDataDetails(summary.getId(), "count", count));
        if (count > 0) {
            metrics.add(metricDataDetails(summary.getId(), "avg", summary.mean()));
            metrics.add(metricDataDetails(summary.getId(), "max", summary.max()));
        }
        return metrics.build();
    }

    /**
     * @param longTaskTimer long task timer meter
     * @return {@link MetricDataDetails} stream with long task timer values
     */
    Stream<MetricDataDetails> trackLongTaskTimer(LongTaskTimer longTaskTimer) {
        return Stream.of(
                metricDataDetails(longTaskTimer.getId(), "activeTasks", longTaskTimer.activeTasks()),
                metricDataDetails(longTaskTimer.getId(), "duration", longTaskTimer.duration(getBaseTimeUnit())));
    }

    /**
     * @param timeGauge timer gauge meter
     * @return {@link MetricDataDetails} stream with timer gauge meter values or null if gauge value is NaN
     */
    Stream<MetricDataDetails> trackTimeGauge(TimeGauge timeGauge) {
        MetricDataDetails metricDatum = metricDataDetails(timeGauge.getId(), "value", timeGauge.value(getBaseTimeUnit()));
        if (metricDatum == null) {
            return Stream.empty();
        }
        return Stream.of(metricDatum);
    }

    /**
     * @param functionCounter function counter meter
     * @return {@link MetricDataDetails} stream with function counter gauge meter values or null if counter value is NaN
     */
    Stream<MetricDataDetails> trackFunctionCounter(FunctionCounter functionCounter) {
        MetricDataDetails metricDataDetails = metricDataDetails(functionCounter.getId(), "count", functionCounter.count());
        if (metricDataDetails == null) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails);
    }

    /**
     * @param functionTimer function timer meter
     * @return {@link MetricDataDetails} stream with function timer meter values or null if timer total time value is
     * not a finite floating-point
     */
    Stream<MetricDataDetails> trackFunctionTimer(FunctionTimer functionTimer) {
        double sum = functionTimer.totalTime(getBaseTimeUnit());
        if (!Double.isFinite(sum)) {
            return Stream.empty();
        }
        Stream.Builder<MetricDataDetails> metrics = Stream.builder();
        double count = functionTimer.count();
        metrics.add(metricDataDetails(functionTimer.getId(), "count", count));
        metrics.add(metricDataDetails(functionTimer.getId(), "sum", sum));
        if (count > 0) {
            metrics.add(metricDataDetails(functionTimer.getId(), "avg", functionTimer.mean(getBaseTimeUnit())));
        }
        return metrics.build();
    }

    /**
     * @param meter meter
     * @return {@link MetricDataDetails} stream with meter values
     */
    Stream<MetricDataDetails> trackMeter(Meter meter) {
        return stream(meter.measure().spliterator(), false)
                .map(ms -> metricDataDetails(meter.getId().withTag(ms.getStatistic()), null, ms.getValue()))
                .filter(Objects::nonNull);
    }

    /**
     * Generates {@link MetricDataDetails}.
     *
     * @param id meter id
     * @param suffix optional suffix to add to the meter id name
     * @param value value
     * @return {@link MetricDataDetails} ready to send to oracle cloud monitoring ingestion endpoint
     */
    MetricDataDetails metricDataDetails(Meter.Id id, @Nullable String suffix, double value) {
        if (Double.isNaN(value)) {
            return null;
        }

        return MetricDataDetails.builder()
                .compartmentId(oracleCloudConfig.compartmentId())
                .name(getMetricName(id, suffix))
                .namespace(oracleCloudConfig.namespace())
                .resourceGroup(oracleCloudConfig.resourceGroup())
                .metadata(id.getDescription() != null ? Collections.singletonMap("description", id.getDescription()) : null)
                .datapoints(Collections.singletonList(
                        Datapoint.builder()
                                .value(value)
                                .timestamp(new Date())
                                .build()))
                .dimensions(toDimensions(id.getConventionTags(config().namingConvention())))
                .build();
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
        if (tag.getKey() == null || tag.getKey().length() == 0 || tag.getValue() == null || tag.getValue().length() == 0) {
            warnThenDebugLogger.log("Tag " + tag.getKey() + " not published because tag key or value are empty.");
            return false;
        }
        return true;
    }
}
