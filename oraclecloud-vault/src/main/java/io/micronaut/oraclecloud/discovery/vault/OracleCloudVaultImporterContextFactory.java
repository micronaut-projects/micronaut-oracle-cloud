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
import com.oracle.bmc.vault.Vaults;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanConfiguration;
import io.micronaut.inject.QualifiedBeanType;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Creates an importer-owned application context for Oracle Cloud Vault imports.
 *
 * @author graemerocher
 * @since 6.0.0
 */
@Internal
final class OracleCloudVaultImporterContextFactory {

    private final Map<Class<?>, Object> singletonOverrides;

    OracleCloudVaultImporterContextFactory() {
        this(Map.of());
    }

    @Internal
    OracleCloudVaultImporterContextFactory(Map<Class<?>, Object> singletonOverrides) {
        this.singletonOverrides = singletonOverrides;
    }

    /**
     * Creates a Vault importer context using normalized declaration-bound properties.
     *
     * @param environment The active environment
     * @param importConfiguration The normalized importer configuration
     * @return The importer-owned context
     */
    OracleCloudVaultImporterContext create(
        Environment environment,
        OracleCloudVaultImportConfiguration importConfiguration,
        @Nullable Secrets secrets,
        @Nullable Vaults vaults) {
        Set<String> activeNames = environment.getActiveNames();
        Map<String, Object> properties = importConfiguration.properties();
        ApplicationContextBuilder builder = ApplicationContext.builder(
            properties,
            activeNames.toArray(String[]::new)
        );
        builder.classLoader(environment.getClassLoader());
        builder.deduceEnvironment(false);
        builder.bootstrapEnvironment(false);
        builder.enableDefaultPropertySources(false);
        builder.eagerInitSingletons(false);
        builder.environmentPropertySource(false);
        builder.beanConfigurationsPredicate(vaultBeanConfigurations());
        builder.beansPredicate(vaultBeans());
        if (!singletonOverrides.isEmpty()) {
            builder.singletons(singletonOverrides.values().toArray());
        }
        ApplicationContext applicationContext = builder.start();

        if (secrets == null) {
            secrets = applicationContext.getBean(Secrets.class);
        }
        if (vaults == null) {
            vaults = applicationContext.getBean(Vaults.class);
        }
        OracleCloudVaultConfiguration configuration = applicationContext.getBean(OracleCloudVaultConfiguration.class);
        List<OracleCloudVaultConfiguration.OracleCloudVault> configVaults = configuration.getVaults();
        if (!configVaults.isEmpty()) {
            OracleCloudVaultConfiguration.OracleCloudVault v = configVaults.getFirst();
            handleIncludesExcludes(importConfiguration, v);
        } else {
            OracleCloudVaultConfiguration.OracleCloudVault newVault = new OracleCloudVaultConfiguration.OracleCloudVault();
            newVault.setCompartmentOcid(importConfiguration.compartmentId());
            newVault.setOcid(importConfiguration.vaultOcid());
            handleIncludesExcludes(importConfiguration, newVault);
            configuration.setVaults(List.of(newVault));
        }
        return new OracleCloudVaultImporterContext(
            applicationContext,
            configuration,
            secrets,
            vaults
        );
    }

    private static void handleIncludesExcludes(OracleCloudVaultImportConfiguration importConfiguration, OracleCloudVaultConfiguration.OracleCloudVault v) {
        List<String> includes = importConfiguration.includes();
        if (CollectionUtils.isNotEmpty(includes)) {
            v.setIncludes(includes.toArray(StringUtils.EMPTY_STRING_ARRAY));
        }
        List<String> excludes = importConfiguration.excludes();
        if (CollectionUtils.isNotEmpty(excludes)) {
            v.setExcludes(excludes.toArray(StringUtils.EMPTY_STRING_ARRAY));
        }
    }

    private static Predicate<BeanConfiguration> vaultBeanConfigurations() {
        return beanConfiguration -> {
            String name = beanConfiguration.getName();
            return supportedPackages(name);
        };
    }

    private static java.util.function.Predicate<QualifiedBeanType<?>> vaultBeans() {
        return beanDefinition -> {
            if (beanDefinition.getBeanType().equals(OracleCloudVaultConfigurationClient.class)) {
                return false;
            }
            String name = beanDefinition.getBeanType().getName();
            return supportedPackages(name);
        };
    }

    private static boolean supportedPackages(String name) {
        return name.startsWith("io.micronaut.oraclecloud")
            || name.startsWith("com.oracle.bmc")
            || name.startsWith("io.micronaut.http")
            || name.startsWith("io.micronaut.jackson")
            || name.startsWith("io.micronaut.json")
            || name.startsWith("io.micronaut.context")
            || name.startsWith("io.micronaut.scheduling")
            || name.startsWith("io.micronaut.core")
            || name.startsWith("io.micronaut.aop")
            || name.startsWith("io.micronaut.runtime")
            || name.startsWith("io.micronaut.serde")
            || name.startsWith("io.micronaut.runtime.converters")
            || name.startsWith("io.micronaut.core.convert")
            || name.startsWith("java.util.concurrent")
            || name.startsWith("reactor.");
    }

    @Internal
    record OracleCloudVaultImporterContext(
        ApplicationContext applicationContext,
        OracleCloudVaultConfiguration configuration,
        Secrets secretsClient,
        Vaults vaultsClient
    ) implements AutoCloseable {
        @Override
        public void close() {
            applicationContext.close();
        }
    }
}
