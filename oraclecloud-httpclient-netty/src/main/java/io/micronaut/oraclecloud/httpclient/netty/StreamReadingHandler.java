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
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.NoSuchElementException;

/**
 * Channel handler that exposes inbound data as an {@link InputStream}.
 */
final class StreamReadingHandler extends ChannelInboundHandlerAdapter {
    private final Object monitor = new Object();
    private CompositeByteBuf buffer;
    private boolean done = false;
    private Throwable failure;

    private ChannelHandlerContext context;

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
    public void handlerAdded(ChannelHandlerContext ctx) {
        synchronized (monitor) {
            this.context = ctx;
            buffer = ctx.alloc().compositeBuffer();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            synchronized (monitor) {
                buffer.addComponent(true, (ByteBuf) msg);
                monitor.notifyAll();
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        synchronized (monitor) {
            done = true;
            monitor.notifyAll();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        synchronized (monitor) {
            if (buffer != null) {
                // the stream hasn't finished yet, it can handle the failure.
                failure = cause;
                buffer.release();
                buffer = null;
                monitor.notifyAll();
                return;
            }
        }
        ctx.fireExceptionCaught(cause);
    }

    private void checkNotOnEventLoop() {
        // embedded channel always returns true for inEventLoop
        if (context.executor().inEventLoop() && !(context.channel() instanceof EmbeddedChannel)) {
            throw new IllegalStateException("This method must not be called on the netty event loop");
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
                        context.read();
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
            try {
                context.pipeline().remove(StreamReadingHandler.this);
            } catch (NoSuchElementException ignored) {
            }
        }
    }
}
