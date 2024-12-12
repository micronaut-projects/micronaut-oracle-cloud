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

import com.oracle.bmc.containerengine.ContainerEngineClient;
import com.oracle.bmc.containerengine.model.CreateClusterKubeconfigContentDetails;
import com.oracle.bmc.containerengine.model.CreateClusterKubeconfigContentDetails.Endpoint;
import com.oracle.bmc.containerengine.requests.CreateKubeconfigRequest;
import com.oracle.bmc.containerengine.responses.CreateKubeconfigResponse;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.kubernetes.client.openapi.config.AbstractKubeConfigLoader;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubeConfigLoader;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for loading a kubeconfig from OKE. It replaces the default kube config loader, which
 * reads it from a file.
 *
 * @since 4.4.x
 * @author Andriy Dmytruk
 */
@Singleton
@BootstrapContextCompatible
@Replaces(KubeConfigLoader.class)
@Requires(beans = OkeKubernetesClientConfig.class)
public class OkeKubeConfigLoader extends AbstractKubeConfigLoader {

    private static final String TOKEN_VERSION = "2.0.0";

    private static final Logger LOG = LoggerFactory.getLogger(OkeKubeConfigLoader.class);
    private final ContainerEngineClient client;
    private final OkeKubernetesClientConfig config;

    /**
     * Create Kubeconfig loader.
     *
     * @param containerEngineClient The client to use to get kubeconfig
     * @param config The configuration
     * @param resourceResolver The resource resolver.
     */
    OkeKubeConfigLoader(
            ContainerEngineClient containerEngineClient,
            OkeKubernetesClientConfig config,
            ResourceResolver resourceResolver
    ) {
        super(resourceResolver);
        this.client = containerEngineClient;
        this.config = config;
    }

    @Override
    protected @Nullable KubeConfig loadKubeConfig() {
        return createCubeConfig(config.clusterId(), config.endpointType());
    }

    /**
     * A method that uses the container engine client to create a kube config and write it to the
     * file specified by the environment variable.
     *
     * @param okeClusterId The cluster ID
     * @param endpointType The endpoint type
     * @return The kubeconfig
     */
    protected KubeConfig createCubeConfig(String okeClusterId, Endpoint endpointType) {
        LOG.info("Creating remote kubeconfig for cluster id {}", okeClusterId);
        CreateClusterKubeconfigContentDetails body = CreateClusterKubeconfigContentDetails.builder()
                .tokenVersion(TOKEN_VERSION)
                .endpoint(endpointType)
                .build();
        CreateKubeconfigRequest kubeConfigRequest = CreateKubeconfigRequest.builder()
                .clusterId(okeClusterId)
                .createClusterKubeconfigContentDetails(body)
                .build();

        CreateKubeconfigResponse response;
        try {
            response = client.createKubeconfig(kubeConfigRequest);
        } catch (Exception e) {
            LOG.error("Caught exception when creating kubeconfig for cluster: {}", okeClusterId, e);
            throw new IllegalStateException("Unable to create KubeConfig", e);
        }
        LOG.info("Successfully received kubeconfig response for cluster id {}", okeClusterId);
        try (InputStream kubeConfig = response.getInputStream()) {
            return loadKubeConfigFromInputStream(kubeConfig);
        } catch (IOException e) {
            LOG.error("Caught exception when reading kubeconfig for cluster {}", okeClusterId, e);
            throw new RuntimeException("Unable to create kubeClient", e);
        }
    }

}
