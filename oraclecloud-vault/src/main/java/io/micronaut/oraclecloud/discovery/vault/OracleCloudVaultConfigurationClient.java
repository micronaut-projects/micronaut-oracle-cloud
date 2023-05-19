/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oraclecloud.discovery.vault;

import com.oracle.bmc.secrets.Secrets;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import com.oracle.bmc.secrets.responses.GetSecretBundleResponse;
import com.oracle.bmc.vault.Vaults;
import com.oracle.bmc.vault.model.SecretSummary;
import com.oracle.bmc.vault.requests.ListSecretsRequest;
import com.oracle.bmc.vault.responses.ListSecretsResponse;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.discovery.config.ConfigurationClient;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * A {@link io.micronaut.discovery.config.ConfigurationClient} for Oracle Cloud Vault Configuration.
 *
 * @author toddsharp
 * @since 1.4.0
 */
@Singleton
@Requires(classes = {
        Secrets.class,
        Vaults.class
})
@Requires(beans = {Vaults.class, Secrets.class})
@Requires(property = OracleCloudVaultConfiguration.PREFIX)
@BootstrapContextCompatible
public class OracleCloudVaultConfigurationClient implements ConfigurationClient {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudVaultConfigurationClient.class);

    private final OracleCloudVaultConfiguration oracleCloudVaultClientConfiguration;
    private final ExecutorService executorService;
    private final Secrets secretsClient;
    private final Vaults vaultsClient;

    /**
     * Default Constructor.
     *
     * @param oracleCloudVaultClientConfiguration Oracle CloudVault Client Configuration
     * @param executorService                     Executor Service
     * @param secretsClient                       The secrets client
     * @param vaultsClient                        The vaults client
     */
    public OracleCloudVaultConfigurationClient(
            OracleCloudVaultConfiguration oracleCloudVaultClientConfiguration,
            @Named(TaskExecutors.IO) @Nullable ExecutorService executorService,
            Secrets secretsClient,
            Vaults vaultsClient) {
        this.oracleCloudVaultClientConfiguration = oracleCloudVaultClientConfiguration;
        this.executorService = executorService;
        this.secretsClient = secretsClient;
        this.vaultsClient = vaultsClient;
    }

    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        if (!oracleCloudVaultClientConfiguration.getDiscoveryConfiguration().isEnabled()) {
            return Flux.empty();
        }

        List<Flux<PropertySource>> propertySources = new ArrayList<>();
        Scheduler scheduler = executorService != null ? Schedulers.fromExecutor(executorService) : null;

        Map<String, Object> secrets = new HashMap<>();

        for (OracleCloudVaultConfiguration.OracleCloudVault vault : oracleCloudVaultClientConfiguration.getVaults()) {
            int retrieved = 0;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieving secrets from Oracle Cloud Vault with OCID: {}", vault.getOcid());
            }
            List<ListSecretsResponse> responses = new ArrayList<>();
            ListSecretsRequest listSecretsRequest = buildRequest(
                    vault.getOcid(),
                    vault.getCompartmentOcid(),
                    null
            );
            ListSecretsResponse listSecretsResponse = vaultsClient.listSecrets(listSecretsRequest);
            responses.add(listSecretsResponse);

            while (listSecretsResponse.getOpcNextPage() != null) {
                listSecretsRequest = buildRequest(
                        vault.getOcid(),
                        vault.getCompartmentOcid(),
                        listSecretsResponse.getOpcNextPage()
                );
                listSecretsResponse = vaultsClient.listSecrets(listSecretsRequest);
                responses.add(listSecretsResponse);
            }

            for (ListSecretsResponse response : responses) {
                retrieved += response.getItems().size();
                response.getItems().forEach(summary -> {
                    byte[] secretValue = getSecretValue(summary.getId());
                    secrets.put(
                            summary.getSecretName(),
                            secretValue
                    );

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Retrieved secret: {}", summary.getSecretName());
                    }
                });
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} secrets where retrieved from Oracle Cloud Vault with OCID: {}", retrieved, vault.getOcid());
            }
        }

        Flux<PropertySource> propertySourceFlowable = Flux.just(
                PropertySource.of(secrets)
        );

        if (scheduler != null) {
            propertySourceFlowable = propertySourceFlowable.subscribeOn(scheduler);
        }
        propertySources.add(propertySourceFlowable);
        return Flux.merge(propertySources);
    }

    private ListSecretsRequest buildRequest(String vaultId, String compartmentId, @Nullable String page) {
        ListSecretsRequest.Builder request = ListSecretsRequest.builder()
                .vaultId(vaultId)
                .compartmentId(compartmentId)
                .lifecycleState(SecretSummary.LifecycleState.Active);
        if (page != null) {
            request.page(page);
        }
        return request.build();
    }

    private byte[] getSecretValue(String secretOcid) {
        GetSecretBundleRequest getSecretBundleRequest = GetSecretBundleRequest
                .builder()
                .secretId(secretOcid)
                .stage(GetSecretBundleRequest.Stage.Current)
                .build();

        GetSecretBundleResponse getSecretBundleResponse = secretsClient.
                getSecretBundle(getSecretBundleRequest);

        Base64SecretBundleContentDetails base64SecretBundleContentDetails =
                (Base64SecretBundleContentDetails) getSecretBundleResponse.
                        getSecretBundle().getSecretBundleContent();

        return Base64.getDecoder().decode(base64SecretBundleContentDetails.getContent());
    }

    @Override
    public String getDescription() {
        return "oraclecloud-vault";
    }
}
