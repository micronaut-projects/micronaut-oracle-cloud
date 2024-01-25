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
import io.micrometer.core.instrument.step.StepDistributionSummary;

import java.util.List;

/**
 * OracleCloudDistributionSummary is {@link StepDistributionSummary} that tracks list of raw {@link Datapoint}.
 */
public class OracleCloudDistributionSummary extends StepDistributionSummary implements OracleCloudDatapointProducer {

    private final DataPointProvider dataPointProvider = new DataPointProvider();

    public OracleCloudDistributionSummary(Id id, Clock clock, DistributionStatisticConfig distributionStatisticConfig, double scale, long stepMillis, boolean supportsAggregablePercentiles) {
        super(id, clock, distributionStatisticConfig, scale, stepMillis, supportsAggregablePercentiles);
    }

    @Override
    protected void recordNonNegative(double amount) {
        super.recordNonNegative(amount);
        dataPointProvider.createDataPoint(amount, 1);
    }

    @Override
    public List<Datapoint> getDatapoints() {
        return dataPointProvider.produceDatapoints();
    }
}
