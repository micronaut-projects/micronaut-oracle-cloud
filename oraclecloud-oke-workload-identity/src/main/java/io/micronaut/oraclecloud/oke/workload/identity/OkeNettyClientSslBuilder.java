/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oraclecloud.oke.workload.identity;

import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder;
import io.micronaut.http.ssl.SslBuilder;
import io.micronaut.http.ssl.SslConfiguration;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.util.Optional;

/**
 * The Netty implementation of {@link SslBuilder} that generates an {@link SslContext} to create a client that
 * supports SSL.<br>
 * This class is not final, so you can extend and replace it to implement alternate mechanisms for loading the
 * key and trust stores.
 */
final class OkeNettyClientSslBuilder extends NettyClientSslBuilder {

    private final TrustManagerFactory trustManagerFactory;
    private final KeyStore keyStore;

    /**
     * @param resourceResolver The resource resolver
     */
    public OkeNettyClientSslBuilder(ResourceResolver resourceResolver, TrustManagerFactory trustManagerFactory, KeyStore keyStore) {
        super(resourceResolver);
        this.trustManagerFactory = trustManagerFactory;
        this.keyStore = keyStore;
    }

    @Override
    protected Optional<KeyStore> getKeyStore(SslConfiguration ssl) {
        return Optional.of(keyStore);
    }

    @Override
    protected TrustManagerFactory getTrustManagerFactory(SslConfiguration ssl) {
        return trustManagerFactory;
    }

}
