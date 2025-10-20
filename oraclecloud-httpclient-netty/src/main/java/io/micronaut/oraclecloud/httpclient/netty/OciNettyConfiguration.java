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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.annotation.Bindable;

/**
 * Configuration properties specific to the managed Netty HTTP client for the OCI SDK.
 * <p>
 * Properties are prefixed with {@code oci.netty}.
 * </p>
 *
 * @param legacyNettyClient           Use the legacy implementation of the Netty client. This is {@link io.micronaut.core.annotation.Experimental}
 *                                    and intended for migration scenarios.
 * @param useManagedProviderGlobally  If {@code true}, enable the managed Netty HTTP provider globally for SDK clients,
 *                                    centralizing HTTP client configuration via Micronaut (for example {@code micronaut.http.*} or {@code oci.client.*}).
 * @author Jonas Konrad
 * @since 4.3.0
 */
@Internal
@ConfigurationProperties(OciNettyConfiguration.PREFIX)
record OciNettyConfiguration(
    @Experimental
    @Bindable(defaultValue = "false")
    boolean legacyNettyClient,
    @Experimental
    @Bindable(defaultValue = "false")
    boolean useManagedProviderGlobally
) {
    static final String PREFIX = "oci.netty";
}
