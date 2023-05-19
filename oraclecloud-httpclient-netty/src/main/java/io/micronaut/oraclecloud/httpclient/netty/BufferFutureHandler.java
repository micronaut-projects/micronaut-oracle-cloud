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

import java.util.concurrent.CompletableFuture;

/**
 * This channel handler accumulates input data ({@link ByteBuf} and
 * {@link io.netty.handler.codec.http.HttpContent}), and when
 * {@link io.netty.handler.codec.http.LastHttpContent} is received completes a {@link #future} with
 * the accumulated data.
 */
final class BufferFutureHandler extends DecidedBodyHandler {
    final CompletableFuture<ByteBuf> future = new CompletableFuture<>();
    private CompositeByteBuf buffer;

    BufferFutureHandler(ByteBufAllocator alloc) {
        buffer = alloc.compositeBuffer();
    }

    @Override
    boolean onError(Throwable cause) {
        if (buffer != null) {
            buffer.release();
            buffer = null;
        }
        return future.completeExceptionally(cause);
    }

    @Override
    void onData(ByteBuf data) {
        buffer.addComponent(true, data);
    }

    @Override
    void onComplete() {
        if (!future.complete(buffer.retain())) {
            buffer.release();
        }
        buffer.release();
        buffer = null;
    }
}
