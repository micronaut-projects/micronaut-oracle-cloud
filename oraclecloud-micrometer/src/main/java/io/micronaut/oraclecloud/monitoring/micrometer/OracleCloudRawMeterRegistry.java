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
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramGauges;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.core.lang.Nullable;
import io.micronaut.http.client.HttpClientRegistry;
import io.micronaut.oraclecloud.monitoring.MonitoringIngestionClient;
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudDatapointProducer;
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudCounter;
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudDistributionSummary;
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudTimer;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link StepMeterRegistry} for Oracle Cloud Monitoring that produces raw data.
 *
 * @author Nemanja Mikic
 * @since 3.6
 */
public class OracleCloudRawMeterRegistry extends AbstractOracleCloudMeterRegistry {

    private final Logger logger = LoggerFactory.getLogger(OracleCloudRawMeterRegistry.class);

    public OracleCloudRawMeterRegistry(HttpClientRegistry<?> httpClientRegistry, OracleCloudConfig oracleCloudConfig,
                                       Clock clock,
                                       Provider<MonitoringIngestionClient> monitoringIngestionClientProvider
                                       ) {
        this(httpClientRegistry, oracleCloudConfig, clock, monitoringIngestionClientProvider, new NamedThreadFactory("oraclecloud-metrics-publisher"));
    }

    public OracleCloudRawMeterRegistry(HttpClientRegistry<?> httpClientRegistry,
                                       OracleCloudConfig oracleCloudConfig,
                                       Clock clock, Provider<MonitoringIngestionClient> monitoringIngestionClientProvider,
                                       ThreadFactory threadFactory) {
        super(httpClientRegistry, oracleCloudConfig, clock, monitoringIngestionClientProvider, threadFactory);

    }

    /**
     * @param id                          The id that uniquely identifies the timer.
     * @param distributionStatisticConfig Configuration for published distribution
     *                                    statistics.
     * @param pauseDetector               The pause detector to use for coordinated omission
     *                                    compensation.
     * @return
     */
    @Override
    public Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig,
                          PauseDetector pauseDetector) {
        Timer timer = new OracleCloudTimer(id, clock, distributionStatisticConfig, pauseDetector, getBaseTimeUnit(),
            this.oracleCloudConfig.step().toMillis(), false);
        HistogramGauges.registerWithCommonFormat(timer, this);
        return timer;
    }

    /**
     * @param id The id that uniquely identifies the counter.
     * @return
     */
    @Override
    public Counter newCounter(Meter.Id id) {
        return new OracleCloudCounter(id, clock, oracleCloudConfig.step().toMillis());
    }

    /**
     * @param id                          The id that uniquely identifies the distribution summary.
     * @param distributionStatisticConfig Configuration for published distribution
     *                                    statistics.
     * @param scale                       Multiply every recorded sample by this factor.
     * @return
     */
    @Override
    public DistributionSummary newDistributionSummary(Meter.Id id,
                                                         DistributionStatisticConfig distributionStatisticConfig, double scale) {
        DistributionSummary summary = new OracleCloudDistributionSummary(id, clock, distributionStatisticConfig, scale,
            oracleCloudConfig.step().toMillis(), false);
        HistogramGauges.registerWithCommonFormat(summary, this);
        return summary;
    }

    @Override
    protected List<MetricDataDetails> getMetricData() {
        return getMeters().stream().flatMap(meter -> meter.match(
            this::trackGauge,
            this::trackRawData,
            this::trackRawData,
            this::trackRawData,
            this::trackLongTaskTimer,
            this::trackTimeGauge,
            this::trackFunctionCounter,
            this::trackFunctionTimer,
            this::trackMeter)
        ).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * @param meter meter
     * @return {@link MetricDataDetails} stream with meter values
     */
    Stream<MetricDataDetails> trackMeter(Meter meter) {
        return StreamSupport.stream(
            meter.measure().spliterator(), false).map(
                (ms) -> this.metricDataDetails(meter.getId().withTag(ms.getStatistic()), null,
                    List.of(Datapoint.builder().timestamp(new Date()).value(ms.getValue()).build())
                )).filter(Objects::nonNull);
    }

    /**
     * @param gauge gauge meter
     * @return {@link MetricDataDetails} stream with gauge values or null if gauge value is NaN
     */
    Stream<MetricDataDetails> trackGauge(Gauge gauge) {
        Double value = gauge.value();
        if (Double.isNaN(value)) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails(gauge.getId(), null, List.of(Datapoint.builder().value(value).timestamp(new Date()).build())));
    }

    /**
     * @param timeGauge timer gauge meter
     * @return {@link MetricDataDetails} stream with timer gauge meter values or null if gauge value is NaN
     */
    Stream<MetricDataDetails> trackTimeGauge(TimeGauge timeGauge) {
        Double value = timeGauge.value(getBaseTimeUnit());
        if (Double.isNaN(value)) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails(timeGauge.getId(), null, List.of(Datapoint.builder().value(value).timestamp(new Date()).build())));
    }

    /**
     * @param functionCounter function counter meter
     * @return {@link MetricDataDetails} stream with function counter gauge meter values or null if counter value is NaN
     */
    Stream<MetricDataDetails> trackFunctionCounter(FunctionCounter functionCounter) {
        Double value = functionCounter.count();
        if (Double.isNaN(value)) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails(functionCounter.getId(), null, List.of(Datapoint.builder().value(value).timestamp(new Date()).build())));
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
        return Stream.of(metricDataDetails(functionTimer.getId(), null, List.of(Datapoint.builder().value(sum).timestamp(new Date()).build())));
    }

    /**
     * @param longTaskTimer long task timer meter
     * @return {@link MetricDataDetails} stream with long task timer values
     */
    Stream<MetricDataDetails> trackLongTaskTimer(LongTaskTimer longTaskTimer) {
        return Stream.of(
            metricDataDetails(
                longTaskTimer.getId(),
                null,
                List.of(Datapoint.builder().value(longTaskTimer.duration(getBaseTimeUnit()))
                    .timestamp(new Date()).build())),
            metricDataDetails(
                longTaskTimer.getId(),
                "activeTasks",
                List.of(Datapoint.builder().value((double) longTaskTimer.activeTasks())
                    .timestamp(new Date()).build()))
        );
    }

    /**
     * @param meter OracleCloudDatapointProducer meter
     * @return {@link MetricDataDetails} stream with multiple entries of {@link Datapoint}
     */
    Stream<MetricDataDetails> trackRawData(Meter meter) {
        if (meter instanceof OracleCloudDatapointProducer oracleCloudDatapointProducer) {
            return Stream.of(metricDataDetails(meter.getId(), null, oracleCloudDatapointProducer.getDatapoints()));
        }
        logger.error("Metrics name: %s. Haven't publish metrics for class: %s".formatted(meter.getId().toString(), meter.getClass()));
        return Stream.empty();
    }

    /**
     * Generates {@link MetricDataDetails}.
     *
     * @param id meter id
     * @param datapoints list of {@link Datapoint}
     * @return {@link MetricDataDetails} ready to send to oracle cloud monitoring ingestion endpoint
     */
    MetricDataDetails metricDataDetails(Meter.Id id, @Nullable String suffix, List<Datapoint> datapoints) {
        if (datapoints.isEmpty()) {
            return null;
        }
        return MetricDataDetails.builder()
            .compartmentId(oracleCloudConfig.compartmentId())
            .name(getMetricName(id, suffix))
            .namespace(oracleCloudConfig.namespace())
            .resourceGroup(oracleCloudConfig.resourceGroup())
            .metadata(oracleCloudConfig.description() && id.getDescription() != null
                ? Map.of("description", id.getDescription()) : null)
            .datapoints(datapoints)
            .dimensions(toDimensions(id.getConventionTags(config().namingConvention())))
            .build();
    }
}
