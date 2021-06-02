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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

/** Utility methods for working with {@link InputStream} and {@link OutputStream}. */
class ByteStreams {
    private static final int BUFFER_SIZE = 65536;

    ByteStreams() { }

    static Reader reader(InputStream content) {
        return new InputStreamReader(content, StandardCharsets.UTF_8);
    }

    byte[] asByteArray(final InputStream is) throws IOException {
        final ReadableByteChannel source = Channels.newChannel(is);
        return asByteArray(source);
    }

    byte[] asByteArray(final ReadableByteChannel source) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final WritableByteChannel destination = Channels.newChannel(os);
            copy(source, destination);
            return os.toByteArray();
        }
    }

    String asString(final InputStream is) throws IOException {
        final byte[] bytes = asByteArray(is);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    InputStream asInputStream(final byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    long copy(final InputStream is, final OutputStream os) throws IOException {
        if (is != null && os != null) {
            final ReadableByteChannel source = Channels.newChannel(is);
            final WritableByteChannel destination = Channels.newChannel(os);
            return copy(source, destination);
        } else {
            return -1;
        }
    }

    private long copy(final ReadableByteChannel source, final WritableByteChannel destination)
            throws IOException {
        long length = 0;
        final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (source.read(buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                /* write() might not write all of the bytes in a single pass */
                length += destination.write(buffer);
            }
            buffer.clear();
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            destination.write(buffer);
        }
        return length;
    }

    InputStream uncloseable(InputStream in) {
        return new Uncloseable(in);
    }

    private static final class Uncloseable extends FilterInputStream {
        Uncloseable(InputStream in) {
            super(in);
        }

        @Override
        public void close() { }
    }
}
