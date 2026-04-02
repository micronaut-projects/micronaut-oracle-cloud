/*
 * Copyright 2017-2026 original authors
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
import io.micronaut.context.env.PropertySource;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal loader that resolves Oracle Cloud Vault secrets into a {@link PropertySource}.
 *
 * @author graemerocher
 * @since 6.0.0
 */
@Internal
final class OracleCloudVaultPropertySourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudVaultPropertySourceLoader.class);

    private final Secrets secretsClient;
    private final Vaults vaultsClient;
    private final OracleCloudVaultConfiguration.OracleCloudVaultClientDiscoveryConfiguration discoveryConfiguration;

    OracleCloudVaultPropertySourceLoader(
        Secrets secretsClient,
        Vaults vaultsClient,
        OracleCloudVaultConfiguration.OracleCloudVaultClientDiscoveryConfiguration discoveryConfiguration
    ) {
        this.secretsClient = secretsClient;
        this.vaultsClient = vaultsClient;
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @NonNull
    PropertySource load(@NonNull List<OracleCloudVaultConfiguration.OracleCloudVault> vaults) {
        Map<String, Object> secrets = new HashMap<>();
        for (OracleCloudVaultConfiguration.OracleCloudVault vault : vaults) {
            LOG.info("Retrieving secrets from Oracle Cloud Vault with OCID: {}", vault.getOcid());
            List<ListSecretsResponse> responses = listSecrets(vault);
            int totalSecrets = responses.stream().mapToInt(response -> response.getItems().size()).sum();
            List<SecretSummary> filteredSecrets = getFilteredListOfItems(responses, vault);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Will retrieve {} of {} secrets from the vault", filteredSecrets.size(), totalSecrets);
            }
            filteredSecrets.forEach(summary -> {
                byte[] secretValue = getSecretValueWithRetry(summary.getId());
                secrets.put(summary.getSecretName(), secretValue);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Retrieved secret: {}", summary.getSecretName());
                }
            });
            LOG.info("{} secrets were retrieved from Oracle Cloud Vault with OCID: {}", filteredSecrets.size(), vault.getOcid());
        }
        return PropertySource.of(secrets);
    }

    private List<ListSecretsResponse> listSecrets(OracleCloudVaultConfiguration.OracleCloudVault vault) {
        List<ListSecretsResponse> responses = new ArrayList<>();
        ListSecretsRequest listSecretsRequest = buildRequest(vault.getOcid(), vault.getCompartmentOcid(), null);
        ListSecretsResponse listSecretsResponse = vaultsClient.listSecrets(listSecretsRequest);
        responses.add(listSecretsResponse);
        while (listSecretsResponse.getOpcNextPage() != null) {
            listSecretsRequest = buildRequest(vault.getOcid(), vault.getCompartmentOcid(), listSecretsResponse.getOpcNextPage());
            listSecretsResponse = vaultsClient.listSecrets(listSecretsRequest);
            responses.add(listSecretsResponse);
        }
        return responses;
    }

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
        for (String exclude : vault.getExcludes()) {
            if (item.getSecretName().matches(exclude)) {
                return true;
            }
        }
        return false;
    }

    private static boolean doesMatchIncludePattern(OracleCloudVaultConfiguration.OracleCloudVault vault, SecretSummary item) {
        for (String include : vault.getIncludes()) {
            if (item.getSecretName().matches(include)) {
                return true;
            }
        }
        return false;
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

    byte[] getSecretValueWithRetry(String secretOcid) {
        Mono<byte[]> mono = Mono.fromCallable(() -> getSecretValue(secretOcid));
        return mono.retryWhen(
            Retry.fixedDelay(discoveryConfiguration.getRetryAttempts(), discoveryConfiguration.getRetryDelay())
                .doAfterRetry(signal -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Error occurred while retrieving secret bundle value for {}, will retry", secretOcid, signal.failure());
                    }
                })
        ).doOnError(ex -> LOG.error("Failed to retrieve secret {}", secretOcid)).block();
    }

    byte[] getSecretValue(String secretOcid) {
        GetSecretBundleRequest getSecretBundleRequest = GetSecretBundleRequest.builder()
            .secretId(secretOcid)
            .stage(GetSecretBundleRequest.Stage.Current)
            .build();
        GetSecretBundleResponse getSecretBundleResponse = secretsClient.getSecretBundle(getSecretBundleRequest);
        Base64SecretBundleContentDetails base64SecretBundleContentDetails =
            (Base64SecretBundleContentDetails) getSecretBundleResponse.getSecretBundle().getSecretBundleContent();
        return Base64.getDecoder().decode(base64SecretBundleContentDetails.getContent());
    }
}
