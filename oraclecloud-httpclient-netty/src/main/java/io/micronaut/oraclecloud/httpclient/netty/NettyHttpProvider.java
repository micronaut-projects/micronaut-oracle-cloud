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

import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.Serializer;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.oraclecloud.serde.OciSdkMicronautSerializer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Netty-based implementation of {@link HttpProvider}. This is only used for SPI
 * ({@link HttpProvider#getDefault()}), there is also {@link ManagedNettyHttpProvider}.
 */
public final class NettyHttpProvider implements HttpProvider {

    private static final AtomicReference<ManagedNettyHttpProvider> MANAGED_NETTY_HTTP_PROVIDER_ATOMIC_REFERENCE = new AtomicReference<>();

    /**
     * Construct a netty-based {@link HttpProvider}.
     */
    public NettyHttpProvider() {
    }

    @Override
    public HttpClientBuilder newBuilder() {
        ManagedNettyHttpProvider p = MANAGED_NETTY_HTTP_PROVIDER_ATOMIC_REFERENCE.get();
        if (p != null) {
            return new NettyHttpClientBuilder(p);
        }
        return new NettyHttpClientBuilder(null);
    }

    @Override
    public Serializer getSerializer() {
        ManagedNettyHttpProvider p = MANAGED_NETTY_HTTP_PROVIDER_ATOMIC_REFERENCE.get();
        if (p != null) {
            return p.getSerializer();
        }
        return OciSdkMicronautSerializer.getDefaultSerializer();
    }

    @Internal
    static void setManagedHttpProvider(@Nullable ManagedNettyHttpProvider managedHttpProvider) {
        NettyHttpProvider.MANAGED_NETTY_HTTP_PROVIDER_ATOMIC_REFERENCE.set(managedHttpProvider);
    }
}
