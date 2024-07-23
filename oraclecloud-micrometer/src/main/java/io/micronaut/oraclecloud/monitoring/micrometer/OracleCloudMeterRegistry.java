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

import com.oracle.bmc.monitoring.model.Datapoint;
import com.oracle.bmc.monitoring.model.MetricDataDetails;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.core.lang.Nullable;
import io.micronaut.http.client.HttpClientRegistry;
import io.micronaut.oraclecloud.monitoring.MonitoringIngestionClient;
import jakarta.inject.Provider;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

/**
 * {@link StepMeterRegistry} for Oracle Cloud Monitoring that produces aggregated data.
 *
 * @author Pavol Gressa
 * @since 1.2
 */
public class OracleCloudMeterRegistry extends AbstractOracleCloudMeterRegistry {

    public OracleCloudMeterRegistry(HttpClientRegistry<?> httpClientRegistry,
                                    OracleCloudConfig oracleCloudConfig,
                                    Clock clock,
                                    Provider<MonitoringIngestionClient> monitoringIngestionClientProvider) {
        this(httpClientRegistry, oracleCloudConfig, clock, monitoringIngestionClientProvider, new NamedThreadFactory("oraclecloud-metrics-publisher"));
    }

    public OracleCloudMeterRegistry(HttpClientRegistry<?> httpClientRegistry,
                                    OracleCloudConfig oracleCloudConfig,
                                    Clock clock,
                                    Provider<MonitoringIngestionClient> monitoringIngestionClientProvider,
                                    ThreadFactory threadFactory) {
        super(httpClientRegistry, oracleCloudConfig, clock, monitoringIngestionClientProvider, threadFactory);
    }

    /**
     * @return list of all {@link Meter} data transformed into {@link MetricDataDetails}
     */
    @Override
    protected List<MetricDataDetails> getMetricData() {
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
        ).filter(Objects::nonNull).collect(Collectors.toList());
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
                .metadata(oracleCloudConfig.description() && id.getDescription() != null
                        ? Collections.singletonMap("description", id.getDescription()) : null)
                .datapoints(Collections.singletonList(
                        Datapoint.builder()
                                .value(value)
                                .timestamp(new Date())
                                .build()))
                .dimensions(toDimensions(id.getConventionTags(config().namingConvention())))
                .build();
    }
}
