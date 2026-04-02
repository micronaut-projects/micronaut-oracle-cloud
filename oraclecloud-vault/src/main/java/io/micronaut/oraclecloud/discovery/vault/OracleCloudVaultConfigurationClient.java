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
import com.oracle.bmc.vault.Vaults;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import org.jspecify.annotations.Nullable;
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
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * A {@link io.micronaut.discovery.config.ConfigurationClient} for Oracle Cloud Vault configuration.
 *
 * @deprecated Use {@code micronaut.config.import[0].provider=oraclecloud-vault} and configure Vault details under {@code oci.vault.*}.
 * @author toddsharp
 * @since 1.4.0
 */
@Deprecated(forRemoval = false, since = "6.0.0")
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
    private final OracleCloudVaultPropertySourceLoader propertySourceLoader;

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
        this.executorService = executorService;
        this.propertySourceLoader = new OracleCloudVaultPropertySourceLoader(
            secretsClient,
            vaultsClient,
            oracleCloudVaultClientConfiguration.getDiscoveryConfiguration()
        );
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
        OracleCloudVaultLegacyDeprecationLogger.warnIfUsed();

        List<Flux<PropertySource>> propertySources = new ArrayList<>();
        Scheduler scheduler = executorService != null ? Schedulers.fromExecutor(executorService) : null;

        Flux<PropertySource> propertySourceFlowable = Flux.just(
            propertySourceLoader.load(oracleCloudVaultClientConfiguration.getVaults())
        );

        if (scheduler != null) {
            propertySourceFlowable = propertySourceFlowable.subscribeOn(scheduler);
        }
        propertySources.add(propertySourceFlowable);
        return Flux.merge(propertySources);
    }

    @Override
    public String getDescription() {
        return "oraclecloud-vault";
    }
}
