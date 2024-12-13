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

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.containerengine.ContainerEngine;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.http.signing.RequestSignerFactory;
import com.oracle.bmc.http.signing.SigningStrategy;
import com.oracle.bmc.http.signing.internal.DefaultRequestSignerFactory;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubeConfigLoader;
import io.micronaut.kubernetes.client.openapi.config.model.ExecConfig;
import io.micronaut.kubernetes.client.openapi.credential.KubernetesTokenLoader;
import io.micronaut.kubernetes.client.openapi.credential.model.ExecCredential;
import io.micronaut.kubernetes.client.openapi.credential.model.ExecCredentialStatus;
import jakarta.inject.Singleton;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A kubernetes credential loader which creates the credentials using a kubeconfig fetched
 * from OKE with {@link OkeKubeConfigLoader}. It uses
 * {@link AbstractAuthenticationDetailsProvider} or {@link RequestSigner} to create
 * the OKE Kubernetes client token.
 *
 * @since 4.4.x
 * @author Andriy Dmytruk
 */
@Singleton
@BootstrapContextCompatible
@Requires(beans = {
    AbstractAuthenticationDetailsProvider.class,
    OkeKubernetesClientConfig.class
})
@Internal
public class OkeKubernetesCredentialLoader implements KubernetesTokenLoader {

    private static final String EXPECTED_COMMAND = "oci";
    private static final String[] EXPECTED_ARGS = {"ce", "cluster", "generate-token"};
    private static final String CLUSTER_ID_ARG = "--cluster-id";
    private static final String REGION_ARG = "--region";

    private static final String DELEGATION_TOKEN_HEADER = "opc-obo-token";
    private static final String AUTHORIZATION_HEADER = "authorization";
    private static final String DATE_HEADER = "date";

    private static final String TOKEN_URL_FORMAT = "%s/cluster_request/%s";
    private static final String EXEC_CREDENTIAL_API_VERSION = "client.authentication.k8s.io/v1beta1";
    private static final String EXEC_CREDENTIAL_KIND = "ExecCredential";

    private static final Logger LOG = LoggerFactory.getLogger(OkeKubernetesCredentialLoader.class);

    /**
     * Higher precedence than for ExecCommandCredentialLoader.
     */
    private static final int ORDER = 5;

    private static final Duration BUFFER = Duration.ofSeconds(60);

    private final String containerEngineEndpoint;
    private final RequestSigner requestSigner;
    private final KubeConfig kubeConfig;

    private volatile ExecCredential execCredential;

    OkeKubernetesCredentialLoader(
            @Nullable RequestSignerFactory requestSignerFactory,
            @NonNull AbstractAuthenticationDetailsProvider authProvider,
            KubeConfigLoader kubeConfigLoader,
            @NonNull ContainerEngine containerEngine
    ) {
        containerEngineEndpoint = containerEngine.getEndpoint();
        if (requestSignerFactory == null) {
            requestSignerFactory = new DefaultRequestSignerFactory(SigningStrategy.STANDARD);
        }
        this.requestSigner = requestSignerFactory.createRequestSigner(null, authProvider);
        this.kubeConfig = kubeConfigLoader.getKubeConfig();
    }

    @Override
    public String getToken() {
        setExecCredential();
        return execCredential == null ? null : execCredential.status().token();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /**
     * Inner method that refreshes the credential if required.
     * It parses parameters from kubeconfig exec command and refreshes token.
     */
    private void setExecCredential() {
        if (kubeConfig == null || kubeConfig.getUser() == null) {
            return;
        }
        ParsedExecCommand command = parseCommand(kubeConfig.getUser().exec());
        if (command == null) {
            return;
        }
        if (shouldLoadCredential()) {
            synchronized (this) {
                if (shouldLoadCredential()) {
                    try {
                        execCredential = loadCredential(command);
                    } catch (Exception e) {
                        LOG.error("Failed to load exec credential", e);
                    }
                }
            }
        }
    }

    /**
     * Parse the exec command provided in the kubeconfig to get the required parameters.
     *
     * @param execConfig The config
     * @return The command
     */
    private ParsedExecCommand parseCommand(ExecConfig execConfig) {
        if (execConfig == null) {
            return null;
        }
        if (!EXPECTED_COMMAND.equals(execConfig.command())) {
            return null;
        }
        List<String> args = execConfig.args();
        for (int i = 0; i < EXPECTED_ARGS.length; i++) {
            if (!EXPECTED_ARGS[i].equals(args.get(i))) {
                return null;
            }
        }
        String clusterId = null;
        String region = null;
        for (int i = EXPECTED_ARGS.length; i < args.size() - 1; i++) {
            if (CLUSTER_ID_ARG.equals(args.get(i))) {
                ++i;
                clusterId = args.get(i);
            }
            if (REGION_ARG.equals(args.get(i))) {
                ++i;
                region = args.get(i);
            }
        }
        if (clusterId == null) {
            throw new IllegalStateException("Cluster ID is required, but was not found in the kubeconfig exec command");
        }
        return new ParsedExecCommand(region, clusterId);
    }

    private boolean shouldLoadCredential() {
        if (execCredential == null) {
            return true;
        }
        ZonedDateTime expiration = execCredential.status().expirationTimestamp();
        if (expiration == null) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        LOG.debug("Check whether credential loading needed, now={}, buffer={}, expiration={}", now, BUFFER, expiration);
        return expiration.isBefore(now.plusSeconds(BUFFER.toSeconds()));
    }

    /**
     * Based on <a href="https://github.com/oracle/oci-cli/blob/6c782b4b7c2c9d43dc737428cb9e2780d37ddc9f/services/container_engine/src/oci_cli_container_engine/containerengine_cli_extended.py#L38">
     *   containerengine_cli_extended.py
     * </a>.
     *
     * @param command The parsed exec command
     * @return The credential
     */
    private ExecCredential loadCredential(ParsedExecCommand command) {
        LOG.debug("Creating OKE kubernetes client credential");
        URI uri = URI.create(String.format(TOKEN_URL_FORMAT, containerEngineEndpoint, command.clusterId));
        Map<String, String> headers = requestSigner.signRequest(uri, "GET", Collections.emptyMap(), null);

        // The authentication headers are formatted inside the URI
        UriBuilder builder = UriBuilder.of(uri)
                .queryParam(AUTHORIZATION_HEADER, headers.get(AUTHORIZATION_HEADER))
                .queryParam(DATE_HEADER, headers.get(DATE_HEADER));
        if (headers.containsKey(DELEGATION_TOKEN_HEADER)) {
            builder.queryParam(DELEGATION_TOKEN_HEADER, headers.get(DELEGATION_TOKEN_HEADER));
        }

        return new ExecCredential(
            EXEC_CREDENTIAL_API_VERSION,
            EXEC_CREDENTIAL_KIND,
            new ExecCredentialStatus(
                    base64Encode(builder.toString()),
                    null,
                    null,
                    ZonedDateTime.now().plusMinutes(4)
            )
        );
    }

    private String base64Encode(String url) {
        ByteBuffer urlBytes = ByteBuffer.wrap(url.getBytes(StandardCharsets.UTF_8));
        // Must have padding
        ByteBuffer encoded = Base64.getUrlEncoder().encode(urlBytes);
        return StandardCharsets.UTF_8.decode(encoded).toString();
    }

    /**
     * A utility data record for the parsed exec command from kube config.
     *
     * @param region The region
     * @param clusterId The cluster id
     */
    private record ParsedExecCommand(
        String region,
        String clusterId
    ) {
    }

}
