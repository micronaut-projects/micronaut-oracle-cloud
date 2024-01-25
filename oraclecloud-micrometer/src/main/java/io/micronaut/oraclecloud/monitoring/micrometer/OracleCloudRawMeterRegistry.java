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
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudDatapointProducer;
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudCounter;
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudDistributionSummary;
import io.micronaut.oraclecloud.monitoring.primitives.OracleCloudTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
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

    public OracleCloudRawMeterRegistry(OracleCloudConfig oracleCloudConfig,
                                    Clock clock,
                                    MonitoringClient monitoringClient) {
        this(oracleCloudConfig, clock, monitoringClient, new NamedThreadFactory("oraclecloud-metrics-publisher"));
    }

    public OracleCloudRawMeterRegistry(OracleCloudConfig oracleCloudConfig,
                                    Clock clock,
                                    MonitoringClient monitoringClient,
                                    ThreadFactory threadFactory) {
        super(oracleCloudConfig, clock, monitoringClient, threadFactory);

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
        ).collect(Collectors.toList());
    }

    Stream<MetricDataDetails> trackMeter(Meter meter) {
        return StreamSupport.stream(meter.measure().spliterator(), false).map((ms) -> this.metricDataDetails(meter.getId().withTag(ms.getStatistic()), List.of(Datapoint.builder().timestamp(new Date()).value(ms.getValue()).build()))).filter(Objects::nonNull);
    }

    Stream<MetricDataDetails> trackGauge(Gauge gauge) {
        Double value = gauge.value();
        if (Double.isNaN(value)) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails(gauge.getId(), List.of(Datapoint.builder().value(value).timestamp(new Date()).build())));
    }

    Stream<MetricDataDetails> trackTimeGauge(TimeGauge timeGauge) {
        Double value = timeGauge.value(getBaseTimeUnit());
        if (Double.isNaN(value)) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails(timeGauge.getId(), List.of(Datapoint.builder().value(value).timestamp(new Date()).build())));
    }

    Stream<MetricDataDetails> trackFunctionCounter(FunctionCounter functionCounter) {
        Double value = functionCounter.count();
        if (Double.isNaN(value)) {
            return Stream.empty();
        }
        return Stream.of(metricDataDetails(functionCounter.getId(), List.of(Datapoint.builder().value(value).timestamp(new Date()).build())));
    }

    Stream<MetricDataDetails> trackFunctionTimer(FunctionTimer functionTimer) {
        return Stream.of(metricDataDetails(functionTimer.getId(), List.of(Datapoint.builder().value(functionTimer.totalTime(getBaseTimeUnit())).count(Double.valueOf(functionTimer.count()).intValue()).timestamp(new Date()).build())));
    }

    Stream<MetricDataDetails> trackLongTaskTimer(LongTaskTimer longTaskTimer) {
        return Stream.of(metricDataDetails(longTaskTimer.getId(), List.of(Datapoint.builder().value(longTaskTimer.duration(getBaseTimeUnit())).count(Double.valueOf(longTaskTimer.activeTasks()).intValue()).timestamp(new Date()).build())));
    }

    Stream<MetricDataDetails> trackRawData(Meter meter) {
        if (meter instanceof OracleCloudDatapointProducer datapointEntryProducer) {
            return Stream.of(metricDataDetails(meter.getId(), datapointEntryProducer.produceDatapoints()));
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
    MetricDataDetails metricDataDetails(Meter.Id id, List<Datapoint> datapoints) {
        if (datapoints.isEmpty()) {
            return null;
        }
        return MetricDataDetails.builder()
            .compartmentId(oracleCloudConfig.compartmentId())
            .name(getMetricName(id, null))
            .namespace(oracleCloudConfig.namespace())
            .resourceGroup(oracleCloudConfig.resourceGroup())
            .metadata(oracleCloudConfig.description() && id.getDescription() != null
                ? Collections.singletonMap("description", id.getDescription()) : null)
            .datapoints(datapoints)
            .dimensions(toDimensions(id.getConventionTags(config().namingConvention())))
            .build();
    }
}
