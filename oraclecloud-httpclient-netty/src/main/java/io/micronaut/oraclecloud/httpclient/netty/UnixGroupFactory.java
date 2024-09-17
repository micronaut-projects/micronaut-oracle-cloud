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

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.netty.channel.DefaultEventLoopGroupFactory;
import io.micronaut.http.netty.channel.EventLoopGroupConfiguration;
import io.micronaut.http.netty.channel.EventLoopGroupFactory;
import io.micronaut.http.netty.channel.NettyChannelType;
import io.micronaut.http.netty.channel.NioEventLoopGroupFactory;
import io.micronaut.http.netty.configuration.NettyGlobalConfiguration;
import io.netty.channel.Channel;
import jakarta.inject.Singleton;

/**
 * {@link EventLoopGroupFactory} that makes the HTTP client use domain socket channels instead of
 * normal nio channels.
 *
 * @author Jonas Konrad
 * @since 4.3.0
 */
@Primary
@Singleton
@Replaces(DefaultEventLoopGroupFactory.class)
@Requires(property = OciNettyConfiguration.PREFIX + ".proxy-domain-socket")
final class UnixGroupFactory extends DefaultEventLoopGroupFactory {
    public UnixGroupFactory(NioEventLoopGroupFactory nioEventLoopGroupFactory, @Nullable EventLoopGroupFactory nativeFactory, @Nullable NettyGlobalConfiguration nettyGlobalConfiguration) {
        super(nioEventLoopGroupFactory, nativeFactory, nettyGlobalConfiguration);
    }

    @Override
    public Channel channelInstance(NettyChannelType type, @Nullable EventLoopGroupConfiguration configuration) {
        if (type == NettyChannelType.CLIENT_SOCKET) {
            type = NettyChannelType.DOMAIN_SOCKET;
        }
        return super.channelInstance(type, configuration);
    }
}
