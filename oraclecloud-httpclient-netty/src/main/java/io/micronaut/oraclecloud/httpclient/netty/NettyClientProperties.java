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

import com.oracle.bmc.http.client.ClientProperty;

/**
 * {@link com.oracle.bmc.http.client.HttpClientBuilder#property(ClientProperty, Object) Client properties}
 * specific to the netty client.
 *
 * @author Jonas Konrad
 */
public final class NettyClientProperties {
    /**
     * The Micronaut HTTP client service ID to use for this client. The client is configured using
     * the {@code micronaut.http.services.<service-id>.*} configuration properties. The default
     * service ID is {@value ManagedNettyHttpProvider#SERVICE_ID}.
     */
    public static final ClientProperty<String> SERVICE_ID = ClientProperty.create("serviceId");

    /**
     * The {@link ManagedNettyHttpProvider} to use for this client.
     */
    public static final ClientProperty<ManagedNettyHttpProvider> MANAGED_PROVIDER = ClientProperty.create("managedProvider");

    private NettyClientProperties() {
    }
}
