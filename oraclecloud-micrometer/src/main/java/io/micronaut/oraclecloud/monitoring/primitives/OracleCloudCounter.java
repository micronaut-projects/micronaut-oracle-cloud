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
import io.micrometer.core.instrument.step.StepCounter;
import io.micronaut.core.annotation.Internal;

import java.util.List;

/**
 * A {@link StepCounter} that tracks list of raw {@link Datapoint}.
 */
@Internal
public final class OracleCloudCounter extends StepCounter implements OracleCloudDatapointProducer {
    private final DataPointProvider dataPointProvider = new DataPointProvider();

    public OracleCloudCounter(Id id, Clock clock, long stepMillis) {
        super(id, clock, stepMillis);
    }

    @Override
    public void increment(double amount) {
        super.increment(amount);
        dataPointProvider.createDataPoint(amount);
    }

    @Override
    public List<Datapoint> getDatapoints() {
        return dataPointProvider.produceDatapoints();
    }
}
