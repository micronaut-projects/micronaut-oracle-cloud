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
package io.micronaut.oraclecloud.certificates.ssl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import java.util.List;

/**
 * Allows for netty SslContext to be delegated to another as well as switched out at runtime.
 */
public class DelegatedSslContext extends SslContext {

    private SslContext ctx;

    /**
     * Creates a new DelegatedSslContext with the SslContext to be delegated to.
     *
     * @param ctx {@link SslContext}
     */
    DelegatedSslContext(SslContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Overrides the existing delegated SslContext with the one passed.
     *
     * @param sslContext {@link SslContext}
     */
    final void setNewSslContext(SslContext sslContext) {
        this.ctx = sslContext;
    }

    @Override
    public final boolean isClient() {
        return ctx.isClient();
    }

    @Override
    public final List<String> cipherSuites() {
        return ctx.cipherSuites();
    }

    @Override
    public final long sessionCacheSize() {
        return ctx.sessionCacheSize();
    }

    @Override
    public final long sessionTimeout() {
        return ctx.sessionTimeout();
    }

    @Override
    public final ApplicationProtocolNegotiator applicationProtocolNegotiator() {
        return ctx.applicationProtocolNegotiator();
    }

    @Override
    public final SSLEngine newEngine(ByteBufAllocator alloc) {
        return ctx.newEngine(alloc);
    }

    @Override
    public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort) {
        return ctx.newEngine(alloc, peerHost, peerPort);
    }

    @Override
    public final SSLSessionContext sessionContext() {
        return ctx.sessionContext();
    }

}
