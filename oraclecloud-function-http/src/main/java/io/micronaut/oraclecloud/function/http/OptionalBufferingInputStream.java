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
package io.micronaut.oraclecloud.function.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This stream class wraps another upstream {@link InputStream}. Normally, read actions are simply
 * forwarded upstream. However, there is an additional action {@link #bufferIfNecessary()} that, if
 * this stream has not been closed yet, buffers all remaining data from the upstream. Downstream
 * consumers can then continue reading data independent of the upstream.
 * <p>This class is necessary because fnproject closes the upstream at some point, and we might not
 * have read all data yet. So we {@link #bufferIfNecessary()} if the downstream users still need
 * access to the data after the close.
 */
final class OptionalBufferingInputStream extends InputStream {
    private final Lock lock = new ReentrantLock();
    private final InputStream upstream;
    private byte[] buffered;
    private int bufferedIndex;
    private boolean closed;

    OptionalBufferingInputStream(InputStream upstream) {
        this.upstream = upstream;
    }

    @Override
    public int read() throws IOException {
        byte[] arr1 = new byte[1];
        int n = read(arr1);
        if (n == -1) {
            return -1;
        } else if (n == 0) {
            throw new IllegalStateException("Read 0 bytes");
        } else {
            return arr1[0] & 0xff;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        lock.lock();
        try {
            if (buffered == null) {
                return upstream.read(b, off, len);
            } else {
                if (bufferedIndex >= buffered.length) {
                    return -1;
                } else {
                    int n = Math.min(len, buffered.length - bufferedIndex);
                    System.arraycopy(buffered, bufferedIndex, b, off, n);
                    bufferedIndex += n;
                    return n;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            closed = true;
            upstream.close();
        } finally {
            lock.unlock();
        }
    }

    void bufferIfNecessary() {
        lock.lock();
        try {
            if (!closed) {
                try {
                    buffered = upstream.readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
