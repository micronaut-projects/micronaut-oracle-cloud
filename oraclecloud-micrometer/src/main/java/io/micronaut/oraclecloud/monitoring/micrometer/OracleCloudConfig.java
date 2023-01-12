/*
 * Copyright 2017-2021 original authors
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

import com.oracle.bmc.monitoring.model.PostMetricDataDetails;
import io.micrometer.core.instrument.config.MeterRegistryConfigValidator;
import io.micrometer.core.instrument.config.validate.InvalidReason;
import io.micrometer.core.instrument.config.validate.PropertyValidator;
import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.core.instrument.step.StepRegistryConfig;

import java.util.regex.Pattern;

import static io.micrometer.core.instrument.config.validate.PropertyValidator.getBoolean;
import static io.micrometer.core.instrument.config.validate.PropertyValidator.getInteger;
import static io.micrometer.core.instrument.config.validate.PropertyValidator.getString;

/**
 * Configuration for {@link OracleCloudMeterRegistry}.
 *
 * @author Pavol Gressa
 * @since 1.2
 */
public interface OracleCloudConfig extends StepRegistryConfig {

    String PREFIX = "oraclecloud";
    String NAMESPACE_REGEX = "^[a-z][a-z0-9_]*[a-z0-9]$";
    Pattern NAMESPACE_PATTERN = Pattern.compile(NAMESPACE_REGEX);

    @Override
    default String prefix() {
        return PREFIX;
    }

    /**
     * @return batch atomicity behaviour
     */
    default PostMetricDataDetails.BatchAtomicity batchAtomicity() {
        return PropertyValidator.getEnum(this, PostMetricDataDetails.BatchAtomicity.class,
                "batchAtomicity").orElse(null);
    }

    /**
     * Application name used as a default dimension for {@link com.oracle.bmc.monitoring.model.MetricDataDetails}.
     *
     * @return application name
     */
    default String applicationName() {
        return getString(this, "applicationName").required().get();
    }

    /**
     * @return metric namespace.
     */
    default String namespace() {
        return getString(this, "namespace").required().get();
    }

    /**
     * @return metrics compartment id, if not provided then OCI SDK tenantId is used.
     */
    default String compartmentId() {
        return getString(this, "compartmentId").required().get();
    }

    /**
     * A resource group is a custom string that can be used as a filter.
     *
     * @return resource group
     */
    default String resourceGroup() {
        return getString(this, "resourceGroup").orElse(null);
    }

    /**
     * Flag whether meter description should be sent to monitoring service.
     *
     * @return description flag
     */
    default boolean description() {
        return getBoolean(this, "description").orElse(true);
    }

    /**
     * @return maximum number of metrics sent in one request, defaults to 50.
     */
    default int batchSize() {
        return getInteger(this, "batchSize").orElse(50);
    }

    @Override
    default Validated<?> validate() {
        return MeterRegistryConfigValidator.checkAll(this,
                c -> StepRegistryConfig.validate(c),
                c -> {
                    if (!NAMESPACE_PATTERN.matcher(namespace()).matches()) {
                        return Validated.invalid(prefix() + ".namespace", namespace(),
                                "must match pattern '" + NAMESPACE_REGEX + "'",
                                InvalidReason.MALFORMED);
                    }
                    if (namespace().startsWith("oci_")) {
                        return Validated.invalid(prefix() + ".namespace", namespace(),
                                "cannot start with 'oci_'", InvalidReason.MALFORMED);
                    }
                    return Validated.valid(prefix() + ".namespace", namespace());
                });
    }
}
