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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.client.netty.NettyClientCustomizer;
import io.netty.bootstrap.Bootstrap;
import jakarta.inject.Singleton;

import java.net.UnixDomainSocketAddress;

/**
 * Customizer that replaces the remote address for proxying through a domain socket.
 *
 * @author Jonas Konrad
 * @since 4.3.0
 */
@Singleton
@Requires(property = OciNettyConfiguration.PREFIX + ".proxy-domain-socket")
final class DomainSocketCustomizer implements BeanCreatedEventListener<NettyClientCustomizer.Registry> {
    private final OciNettyConfiguration configuration;

    DomainSocketCustomizer(OciNettyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public NettyClientCustomizer.Registry onCreated(@NonNull BeanCreatedEvent<NettyClientCustomizer.Registry> event) {
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(configuration.proxyDomainSocket());
        event.getBean().register(new NettyClientCustomizer() {
            @Override
            public @NonNull NettyClientCustomizer specializeForBootstrap(@NonNull Bootstrap bootstrap) {
                bootstrap.remoteAddress(address);
                return this;
            }
        });
        return event.getBean();
    }
}
