/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.oraclecloud.serialization.jackson.internal;

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.oracle.bmc.http.client.InternalSdk;

import java.text.FieldPosition;
import java.util.Date;

/**
 * Swagger uses RFC3339 formats for date. By default, Jackson's StdDateFormatter will use a format
 * that is not exactly compatible (ex, uses hour offsets instead of 'Z').
 *
 * <p>Leave deserialization alone, only take over serialization.
 */
@InternalSdk
@SuppressWarnings({"deprecation"})
public class Rfc3339DateFormat extends StdDateFormat {

    public Rfc3339DateFormat() {

    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        // Same as ISO8601DateFormat but we always serialize millis
        toAppendTo.append(ISO8601Utils.format(date, true));
        return toAppendTo;
    }

    @Override
    public Rfc3339DateFormat clone() {
        return new Rfc3339DateFormat();
    }

}
