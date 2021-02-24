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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.lang.Nullable;
import io.micrometer.core.util.internal.logging.WarnThenDebugLogger;

import java.util.regex.Pattern;

/**
 * @link NamingConvention} for Oracle Cloud Monitoring.
 *
 * @author Pavol Gressa
 * @see <a href="https://docs.oracle.com/en-us/iaas/tools/java/1.32.0/com/oracle/bmc/monitoring/model/MetricDataDetails.html">MetricDataDetails</a>
 * @since 1.2
 */
public class OracleCloudMetricsNamingConvention implements NamingConvention {

    private static final Pattern NAME_CHARS = Pattern.compile("[^a-zA-Z0-9._\\-$]");
    private static final Pattern TAG_KEY_SPECIAL_CHARS = Pattern.compile("[. ]");
    private static final Pattern PRINTABLE_CHARS = Pattern.compile("[^\\p{Print}]");

    private static final int DIMENSION_NAME_MAX_LENGTH = 256;

    private final WarnThenDebugLogger warnThenDebugLogger = new WarnThenDebugLogger(OracleCloudMetricsNamingConvention.class);

    /**
     * A valid name value starts with an alphabetical character and includes only alphanumeric characters, dots,
     * underscores, hyphens, and dollar signs. The `oci_` prefix is reserved. Avoid entering confidential information.
     *
     * @param name     name
     * @param type     type
     * @param baseUnit baseUnit
     * @return sanitized name
     */
    @Override
    public String name(String name, Meter.Type type, @Nullable String baseUnit) {
        String sanitized = name;
        if (sanitized.startsWith("oci_")) {
            sanitized = "m_" + sanitized;
            warnThenDebugLogger.log("Prefix 'm_' added to the meter name " + name + " as the 'oci_' prefix is reserved");
        }

        sanitized = NAME_CHARS.matcher(sanitized).replaceAll("_");

        if (!Character.isLetter(sanitized.charAt(0))) {
            sanitized = "m_" + sanitized;
            warnThenDebugLogger.log("Prefix 'm_' added to the meter " + name + " as valid dimension key starts with alphabetical character");
        }
        return sanitized;
    }

    /**
     * A valid dimension key includes only printable ASCII, excluding periods (.) and spaces. The character limit for
     * a dimension key is 256.
     *
     * @param key key
     * @return sanitized tag key
     */
    @Override
    public String tagKey(String key) {
        String sanitized = PRINTABLE_CHARS.matcher(key).replaceAll("_");
        sanitized = TAG_KEY_SPECIAL_CHARS.matcher(sanitized).replaceAll("_");
        if (sanitized.length() > DIMENSION_NAME_MAX_LENGTH) {
            sanitized = sanitized.substring(0, DIMENSION_NAME_MAX_LENGTH);
            warnThenDebugLogger.log("Trimmed tag key " + key + " to maximum allowed length " + DIMENSION_NAME_MAX_LENGTH + " chars");
        }
        return sanitized;
    }

    /**
     * A valid dimension value includes only Unicode characters. The character limit for a dimension value is 256.
     * Empty strings are not allowed for keys or values. Avoid entering confidential information.
     *
     * @param value value
     * @return sanitized tag value
     */
    @Override
    public String tagValue(String value) {
        String sanitized = PRINTABLE_CHARS.matcher(value).replaceAll("_");
        if (sanitized.length() > DIMENSION_NAME_MAX_LENGTH) {
            sanitized = sanitized.substring(0, DIMENSION_NAME_MAX_LENGTH);
            warnThenDebugLogger.log("Trimmed tag value " + value + " to maximum allowed length " + DIMENSION_NAME_MAX_LENGTH + " chars");
        }
        return sanitized;
    }
}
