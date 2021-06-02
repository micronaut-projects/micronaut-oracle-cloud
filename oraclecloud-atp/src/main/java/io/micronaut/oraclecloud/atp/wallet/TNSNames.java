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
package io.micronaut.oraclecloud.atp.wallet;

import oracle.net.jdbc.nl.NLException;
import oracle.net.jdbc.nl.NLParamParser;
import oracle.net.jdbc.nl.NVPair;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Represents the contents of a {@code TNSNAMES.ORA} file */
final class TNSNames {
    static final String NAME = "tnsnames.ora";

    private final Map<String, ConnectionDescriptor> connectionDescriptors;

    private TNSNames(final Map<String, ConnectionDescriptor> connectionDescriptors) {
        this.connectionDescriptors =
                Collections.unmodifiableMap(
                        (connectionDescriptors != null)
                                ? new TreeMap<>(connectionDescriptors)
                                : new TreeMap<>());
    }

    /**
     * Read the contents of {@code TNSNAMES.ORA}. Note we use an undocumented API of the Oracle JDBC
     * Driver to do this, {@link NLParamParser}.
     *
     * @param content The content of the resource
     * @return {@link TNSNames} instance
     * @throws IOException if an error occurs reading or parsing the TNSNAMES.ora resource
     */
    static final TNSNames read(final InputStream content) throws IOException {
        try (Reader r = ByteStreams.reader(content)) {
            Map<String, ConnectionDescriptor> connectionDescriptors = new LinkedHashMap<>();
            final NLParamParser parser = new NLParamParser(r);
            for (String serviceAlias : parser.getNLPAllNames()) {
                final NVPair value = parser.getNLPListElement(serviceAlias);
                final ConnectionDescriptor connectionDescriptor =
                        new ConnectionDescriptor(value.valueToString());
                connectionDescriptors.put(serviceAlias, connectionDescriptor);
            }
            return new TNSNames(connectionDescriptors);
        } catch (NLException e) {
            throw new IOException(e);
        }
    }

    public boolean isEmpty() {
        return connectionDescriptors.isEmpty();
    }

    public Collection<ConnectionDescriptor> tnsEntries() {
        return Collections.unmodifiableCollection(connectionDescriptors.values());
    }

    /**
     * Find the corresponding {@link ConnectionDescriptor} for the specified service alias
     *
     * @param serviceAlias The alias for the {@link ConnectionDescriptor} to find
     * @return {@link ConnectionDescriptor} instance or null if no match found
     */
    ConnectionDescriptor connectionDescriptor(final String serviceAlias) {
        /* NLParamParser uppercases service aliases */
        final String normalized = serviceAlias.toUpperCase();
        final ConnectionDescriptor connectionDescriptor = connectionDescriptors.get(normalized);
        return connectionDescriptor;
    }
}
