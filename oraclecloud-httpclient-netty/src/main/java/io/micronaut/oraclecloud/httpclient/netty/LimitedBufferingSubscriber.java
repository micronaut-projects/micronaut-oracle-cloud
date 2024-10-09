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
package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.io.buffer.ReferenceCounted;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * {@link Subscriber} implementation that buffers its input up to a certain number of bytes.
 *
 * @author Jonas Konrad
 * @since 4.3
 */
final class LimitedBufferingSubscriber implements Subscriber<ByteBuffer<?>>, Closeable {
    final CompletableFuture<byte[]> future = new CompletableFuture<>();

    private final int maxBuffer;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private boolean closed;
    private Subscription subscription;

    LimitedBufferingSubscriber(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    @Override
    public void onSubscribe(Subscription s) {
        boolean closed;
        synchronized (this) {
            closed = this.closed;
            if (!closed) {
                this.subscription = s;
            }
        }
        if (closed) {
            s.cancel();
        }
    }

    @Override
    public void onNext(ByteBuffer<?> byteBuffer) {
        try {
            byteBuffer.toInputStream().transferTo(buffer);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        if (byteBuffer instanceof ReferenceCounted rc) {
            rc.release();
        }
        if (buffer.size() >= maxBuffer) {
            future.completeExceptionally(new IOException("Request body was streamed and too large for opportunistic buffering"));
            subscription.cancel();
        } else {
            subscription.request(1);
        }
    }

    @Override
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(buffer.toByteArray());
    }

    @Override
    public void close() {
        synchronized (this) {
            if (!closed) {
                if (subscription != null) {
                    subscription.cancel();
                    subscription = null;
                }
                closed = true;
            }
        }
    }
}
