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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Channel handler that writes data from a given {@link InputStream} to the channel.
 */
final class StreamWritingHandler extends ChannelInboundHandlerAdapter {
    public static final int MAX_WRITE_TARGET = 1024 * 16;
    private final InputStream stream;
    private final ExecutorService blockingIoExecutor;
    private final Object terminationMessage;

    private boolean done = false;
    private Future<?> currentFuture;

    /**
     * @param stream             Input data
     * @param blockingIoExecutor Executor to run blocking {@link InputStream#read()} operations on
     * @param terminationMessage Message to send through the pipeline when all data has been written
     */
    StreamWritingHandler(InputStream stream, ExecutorService blockingIoExecutor, Object terminationMessage) {
        this.stream = stream;
        this.blockingIoExecutor = blockingIoExecutor;
        this.terminationMessage = terminationMessage;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        writeIfPossible(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        writeIfPossible(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (currentFuture != null) {
            currentFuture.cancel(true);
        }
    }

    private void writeIfPossible(ChannelHandlerContext ctx) {
        if (!done && currentFuture == null && ctx.channel().isWritable()) {
            long bytesBeforeUnwritable = ctx.channel().bytesBeforeUnwritable();
            int writeTarget = bytesBeforeUnwritable < MAX_WRITE_TARGET ? (int) bytesBeforeUnwritable : MAX_WRITE_TARGET;
            currentFuture = blockingIoExecutor.submit(new WriteTask(ctx, writeTarget));
        }
    }

    private void complete(ChannelHandlerContext ctx) {
        done = true;
        ctx.writeAndFlush(terminationMessage);
        ctx.pipeline().remove(this);
    }

    private class WriteTask implements Runnable {
        private final ChannelHandlerContext ctx;
        private final int targetCount;

        WriteTask(ChannelHandlerContext ctx, int targetCount) {
            this.ctx = ctx;
            this.targetCount = targetCount;
        }

        @Override
        public void run() {
            ByteBuf target = ctx.alloc().heapBuffer(targetCount);
            try {
                int read = stream.read(target.array(), target.arrayOffset() + target.writerIndex(), target.writableBytes());
                if (read == -1) {
                    ctx.channel().eventLoop().execute(() -> {
                        currentFuture = null;
                        complete(ctx);
                    });
                } else {
                    target.writerIndex(target.writerIndex() + read);
                    target.retain();
                    ctx.channel().eventLoop().execute(() -> {
                        currentFuture = null;
                        ctx.writeAndFlush(target, ctx.voidPromise());
                        writeIfPossible(ctx);
                    });
                }
            } catch (InterruptedIOException e) {
                // ignore
            } catch (Exception e) {
                ctx.fireExceptionCaught(e);
            } finally {
                target.release();
            }
        }
    }
}
