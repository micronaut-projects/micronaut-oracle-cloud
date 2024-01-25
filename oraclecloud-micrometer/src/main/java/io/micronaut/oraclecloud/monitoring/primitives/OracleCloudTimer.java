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
package io.micronaut.oraclecloud.monitoring.primitives;

import com.oracle.bmc.monitoring.model.Datapoint;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.step.StepTimer;
import io.micrometer.core.instrument.util.TimeUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OracleCloudTimer is {@link StepTimer} that tracks list of raw {@link Datapoint}.
 */
public class OracleCloudTimer extends StepTimer implements OracleCloudDatapointProducer {
    private final TimeUnit timeUnit;
    private final DataPointProvider dataPointProvider = new DataPointProvider();

    public OracleCloudTimer(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector, TimeUnit baseTimeUnit, long stepDurationMillis, boolean supportsAggregablePercentiles) {
        super(id, clock, distributionStatisticConfig, pauseDetector, baseTimeUnit, stepDurationMillis, supportsAggregablePercentiles);
        this.timeUnit = baseTimeUnit;
    }

    @Override
    protected void recordNonNegative(final long amount, final TimeUnit unit) {
        super.recordNonNegative(amount, unit);
        final long nanoAmount = (long) TimeUtils.convert(amount, unit, TimeUnit.NANOSECONDS);
        dataPointProvider.createDataPoint(TimeUtils.nanosToUnit(nanoAmount, timeUnit));
    }

    @Override
    public List<Datapoint> getDatapoints() {
        return dataPointProvider.produceDatapoints();
    }
}
