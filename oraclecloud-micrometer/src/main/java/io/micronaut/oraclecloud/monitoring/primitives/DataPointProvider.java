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
import io.micrometer.core.instrument.step.StepCounter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DataPointProvider stores the {@link Datapoint}.
 */
final class DataPointProvider {

    private List<Datapoint> datapoints = new ArrayList<>();

    /**
     * Produces the list of datapoints that will be sent. It will also preform cleanup
     * of the internal array
     *
     * @return list of {@link Datapoint}
     */
    public synchronized List<Datapoint> produceDatapoints() {
        ArrayList<Datapoint> datapointsToReturn = new ArrayList<>(datapoints);
        datapoints = new ArrayList<>();
        return datapointsToReturn;
    }

    /**
     * Creates {@link Datapoint} based on value.
     * @param value of the datapoint
     */
    void createDataPoint(Double value) {
        if (Double.isNaN(value)) {
            return;
        }
        synchronized (this) {
            datapoints.add(Datapoint.builder().timestamp(new Date()).value(value).build());
        }
    }

    /**
     * Creates {@link Datapoint} based on value.
     * @param value of the datapoint
     * @param count of the datapoint
     */
    synchronized void createDataPoint(Double value, Integer count) {
        datapoints.add(Datapoint.builder().timestamp(new Date()).value(value).count(count).build());
    }
}
