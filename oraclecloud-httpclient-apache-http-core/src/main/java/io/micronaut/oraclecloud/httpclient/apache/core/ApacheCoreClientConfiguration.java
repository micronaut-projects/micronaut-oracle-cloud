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
package io.micronaut.oraclecloud.httpclient.apache.core;


import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;

/**
 * Configuration for the Apache Core client uses for UDS requests.
 *
 * @param proxyDomainSocket The proxy domain socket to use.
 */
@ConfigurationProperties(ApacheCoreClientConfiguration.PREFIX)
@BootstrapContextCompatible
@Requires(property = ApacheCoreClientConfiguration.PREFIX)
public record ApacheCoreClientConfiguration(
    @Nullable String proxyDomainSocket
) {
    public static final String PREFIX = "oci.apache-core-client";
}
