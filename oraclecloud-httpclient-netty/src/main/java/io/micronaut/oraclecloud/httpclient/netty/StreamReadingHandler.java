/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * Channel handler that exposes inbound data as an {@link InputStream}.
 */
class StreamReadingHandler extends DecidedBodyHandler {
    private final Object monitor = new Object();
    private CompositeByteBuf buffer;
    private boolean done = false;
    private Throwable failure;

    StreamReadingHandler(ByteBufAllocator alloc) {
        synchronized (monitor) {
            buffer = alloc.compositeBuffer();
        }
    }

    public InputStream getInputStream() throws Throwable {
        synchronized (monitor) {
            if (buffer == null) {
                if (failure != null) {
                    throw failure;
                }
                throw new IllegalStateException("Must be added to pipeline first");
            }
            return new Stream();
        }
    }

    @Override
    void onData(ByteBuf data) {
        synchronized (monitor) {
            buffer.addComponent(true, data);
            monitor.notifyAll();
        }
    }

    @Override
    void onComplete() {
        synchronized (monitor) {
            done = true;
            monitor.notifyAll();
        }
    }

    @Override
    boolean onError(Throwable cause) {
        synchronized (monitor) {
            if (buffer != null) {
                // the stream hasn't finished yet, it can handle the failure.
                failure = cause;
                buffer.release();
                buffer = null;
                monitor.notifyAll();
                return true;
            } else {
                return false;
            }
        }
    }

    private class Stream extends InputStream {
        @Override
        public int read() throws IOException {
            byte[] bytes = new byte[1];
            int n = read(bytes, 0, 1);
            if (n == -1) {
                return -1;
            }
            assert n == 1;
            return bytes[0] & 0xff;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            synchronized (monitor) {
                while (true) {
                    if (failure != null) {
                        throw new IOException("Failure in netty event loop", failure);
                    } else if (buffer == null) {
                        return -1;
                    } else if (buffer.isReadable()) {
                        int read = Math.min(len, buffer.readableBytes());
                        buffer.readBytes(b, off, read);
                        buffer.discardSomeReadBytes();
                        return read;
                    } else if (done) {
                        buffer.release();
                        buffer = null;
                    } else {
                        triggerUpstreamRead();
                        checkNotOnEventLoop();
                        try {
                            monitor.wait();
                        } catch (InterruptedException e) {
                            throw new InterruptedIOException();
                        }
                    }
                }
            }
        }

        @Override
        public int available() throws IOException {
            synchronized (monitor) {
                if (buffer == null) {
                    return 0;
                }
                return buffer.readableBytes();
            }
        }

        @Override
        public void close() throws IOException {
            removeEarly();
        }
    }
}
