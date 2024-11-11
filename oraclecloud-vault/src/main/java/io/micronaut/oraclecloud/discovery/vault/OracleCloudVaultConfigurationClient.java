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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

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
    private final OracleCloudVaultConfiguration.OracleCloudVaultClientDiscoveryConfiguration discoveryConfiguration;

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
            @Named(TaskExecutors.BLOCKING) @Nullable ExecutorService executorService,
            Secrets secretsClient,
            Vaults vaultsClient) {
        this.oracleCloudVaultClientConfiguration = oracleCloudVaultClientConfiguration;
        this.discoveryConfiguration = oracleCloudVaultClientConfiguration.getDiscoveryConfiguration();
        this.executorService = executorService;
        this.secretsClient = secretsClient;
        this.vaultsClient = vaultsClient;
    }

    /**
     * Retrieves a publisher of property sources from the Oracle Cloud Vault configuration.<br/>
     * <br/>
     * This method iterates over the list of vaults defined in the Oracle Cloud Vault configuration,
     * retrieves the secrets from each vault using the Oracle Cloud Vault API, filters the secrets
     * based on the include and exclude patterns defined in the vault configuration, and returns a
     * publisher of property sources containing the filtered secrets.<br/>
     * <br/>
     * If the discovery configuration is disabled, an empty publisher is returned.
     *
     * @param environment the Micronaut environment
     * @return a publisher of property sources containing the filtered secrets
     */
    @Override
    public Publisher<PropertySource> getPropertySources(Environment environment) {
        if (!oracleCloudVaultClientConfiguration.getDiscoveryConfiguration().isEnabled()) {
            return Flux.empty();
        }

        List<Flux<PropertySource>> propertySources = new ArrayList<>();
        Scheduler scheduler = executorService != null ? Schedulers.fromExecutor(executorService) : null;

        Map<String, Object> secrets = new HashMap<>();

        oracleCloudVaultClientConfiguration.getVaults().forEach(vault -> {
            LOG.info("Retrieving secrets from Oracle Cloud Vault with OCID: {}", vault.getOcid());

            List<ListSecretsResponse> responses = new ArrayList<>();
            ListSecretsRequest listSecretsRequest = buildRequest(
                vault.getOcid(),
                vault.getCompartmentOcid(),
                null
            );
            ListSecretsResponse listSecretsResponse = vaultsClient.listSecrets(listSecretsRequest);
            responses.add(listSecretsResponse);

            int totalSecrets = listSecretsResponse.getItems().size();
            while (listSecretsResponse.getOpcNextPage() != null) {
                listSecretsRequest = buildRequest(
                    vault.getOcid(),
                    vault.getCompartmentOcid(),
                    listSecretsResponse.getOpcNextPage()
                );
                listSecretsResponse = vaultsClient.listSecrets(listSecretsRequest);
                responses.add(listSecretsResponse);
                totalSecrets += listSecretsResponse.getItems().size();
            }

            // Filter the responses based on the configured includes and excludes
            List<SecretSummary> filteredSecrets = getFilteredListOfItems(responses, vault);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Will retrieve {} of {} secrets from the vault", filteredSecrets.size(), totalSecrets);
            }

            // Iterate the summaries list and store the decoded value for each secret
            filteredSecrets.forEach(summary -> {
                byte[] secretValue = getSecretValueWithRetry(summary.getId());
                secrets.put(summary.getSecretName(), secretValue);

                if (LOG.isTraceEnabled()) {
                    LOG.trace("Retrieved secret: {}", summary.getSecretName());
                }
            });
            LOG.info("{} secrets were retrieved from Oracle Cloud Vault with OCID: {}", filteredSecrets.size(), vault.getOcid());
        });

        Flux<PropertySource> propertySourceFlowable = Flux.just(
            PropertySource.of(secrets)
        );

        if (scheduler != null) {
            propertySourceFlowable = propertySourceFlowable.subscribeOn(scheduler);
        }
        propertySources.add(propertySourceFlowable);
        return Flux.merge(propertySources);
    }

    /**
     * Filters the list of SecretSummaries based on the include and exclude patterns defined in the OracleCloudVault object.
     *
     * @param responses the list of ListSecretsResponses containing the SecretSummaries to filter
     * @param vault     the OracleCloudVault object defining the include and exclude patterns
     * @return a list of filtered SecretSummaries matching the specified criteria
     */
    private List<SecretSummary> getFilteredListOfItems(List<ListSecretsResponse> responses, OracleCloudVaultConfiguration.OracleCloudVault vault) {
        List<SecretSummary> filteredList = new ArrayList<>();
        responses.forEach(response -> response.getItems().forEach(item -> {
            boolean matchesIncludePattern = doesMatchIncludePattern(vault, item);
            boolean matchesExcludePattern = doesMatchExcludePattern(vault, item);

            if ((vault.getIncludes().length == 0 || matchesIncludePattern) && !matchesExcludePattern) {
                filteredList.add(item);
            }
        }));
        return filteredList;
    }

    private static boolean doesMatchExcludePattern(OracleCloudVaultConfiguration.OracleCloudVault vault, SecretSummary item) {
        boolean matchesExcludePattern = false;
        for (String exclude : vault.getExcludes()) {
            if (item.getSecretName().matches(exclude)) {
                matchesExcludePattern = true;
                break;
            }
        }
        return matchesExcludePattern;
    }

    private static boolean doesMatchIncludePattern(OracleCloudVaultConfiguration.OracleCloudVault vault, SecretSummary item) {
        boolean matchesIncludePattern = false;
        for (String include : vault.getIncludes()) {
            if (item.getSecretName().matches(include)) {
                matchesIncludePattern = true;
                break;
            }
        }
        return matchesIncludePattern;
    }

    /**
     * Builds a ListSecretsRequest instance with the specified parameters.
     *
     * @param vaultId       the ID of the vault to retrieve secrets from
     * @param compartmentId the ID of the compartment where the vault resides
     * @param page          the pagination token to use for retrieving the next page of results, or null to start from the first page
     * @return a ListSecretsRequest instance configured with the specified parameters
     */
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

    /**
     * Get the secret bundle with retry because getSecretValue will occasionally fail, and during bootstrap
     * we should not fail. Since we just got the secret id from the listSecrets endpoint, we can
     * presume it is likely a transient failure.
     *
     * @param secretOcid the ocid of the secret to fetch
     * @return the decoded content of the secret byndle
     */
    byte[] getSecretValueWithRetry(String secretOcid) {
        Mono<byte[]> mono = Mono.fromCallable(() -> getSecretValue(secretOcid));

        return mono.retryWhen(
            Retry.fixedDelay(
                discoveryConfiguration.getRetryAttempts(),
                discoveryConfiguration.getRetryDelay()
            ).doAfterRetry(signal -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error occurred while retrieving secret bundle value for {}, will retry", secretOcid, signal.failure());
                }
            })
        ).doOnError(ex -> LOG.error("Failed to retrieve secret {}", secretOcid)).block();
    }

    /**
     * Get the decoded value from the secret bundle.
     *
     * @param secretOcid the ocid of the secret to fetch
     * @return the decoded content of the secret byndle
     */
    byte[] getSecretValue(String secretOcid) {
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
