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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.annotation.Nullable;

import java.nio.file.Path;

/**
 * Configuration properties specific to the managed client.
 *
 * @param proxyDomainSocket A domain socket to send all requests through. The requests will be sent
 *                          as HTTP (no TLS) and with absolute-form URIs.
 * @author Jonas Konrad
 * @since 4.3.0
 */
@ConfigurationProperties(OciNettyConfiguration.PREFIX)
record OciNettyConfiguration(
    @Experimental
    @Nullable
    Path proxyDomainSocket
) {
    static final String PREFIX = "oci.netty";
}
