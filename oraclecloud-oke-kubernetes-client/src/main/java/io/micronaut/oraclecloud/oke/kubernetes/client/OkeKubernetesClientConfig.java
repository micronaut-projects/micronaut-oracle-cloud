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
package io.micronaut.oraclecloud.oke.kubernetes.client;

import com.oracle.bmc.containerengine.model.CreateClusterKubeconfigContentDetails.Endpoint;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.util.StringUtils;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for the OKE kubernetes client.
 * If enabled, the client will retrieve a kubeconfig from the cluster and sign tokens
 * with the OCI SDK authentication. This is not required on kubernetes nodes, as there
 * kubeconfig should already be present.
 *
 * @param enabled Whether the client is enabled
 * @param clusterId The OKE cluster ID
 * @param endpointType Define which endpoint type to use. One of {@code PublicEndpoint},
 *                     {@code PrivateEndpoint}, {@code VcnHostname} and {@code LegacyKubernetes}.
 *                     Default is {@code PublicEndpoint}.
 *
 * @since 4.4.x
 * @author Andriy Dmytruk
 */
@Requires(property = OkeKubernetesClientConfig.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
@Requires(property = OkeKubernetesClientConfig.CLUSTER_ID)
@ConfigurationProperties(OkeKubernetesClientConfig.PREFIX)
@BootstrapContextCompatible
public record OkeKubernetesClientConfig(
        @Bindable(defaultValue = StringUtils.TRUE)
        boolean enabled,
        @NonNull @NotBlank
        String clusterId,
        @Nullable @Bindable(defaultValue = "PublicEndpoint")
        Endpoint endpointType
) {

    public static final String PREFIX = "oci.oke.kubernetes.client";
    public static final String ENABLED = PREFIX + ".enabled";
    public static final String CLUSTER_ID = PREFIX + ".cluster-id";

}
