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
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import io.micronaut.core.annotation.Internal;

import java.util.List;

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
     * The Oci Netty client filter property contains list of {@link OciNettyClientFilter} that will be executed
     * during sending the request. The {@link OciNettyClientFilter#beforeRequest(HttpRequest)} will be executed
     * before sending and {@link OciNettyClientFilter#afterResponse(HttpRequest, HttpResponse, Throwable, Object)}
     * will be executed after response is received from the server.
     */
    public static final ClientProperty<List<OciNettyClientFilter<?>>> OCI_NETTY_CLIENT_FILTERS_KEY = ClientProperty.create("ociNettyClientFilters");

    /**
     * The Client attribute that stores class name with method of the client that invoked request.
     */
    public static final String CLASS_AND_METHOD_KEY_NAME = "class_and_method";

    /**
     * The {@link ManagedNettyHttpProvider} to use for this client.
     */
    @Internal
    public static final ClientProperty<ManagedNettyHttpProvider> MANAGED_PROVIDER = ClientProperty.create("managedProvider");

    private NettyClientProperties() {
    }
}
