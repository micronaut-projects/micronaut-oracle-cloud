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
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanConfiguration;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanDefinitionReference;
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
    private static final String LEGACY_VAULT_CONFIGURATION_CLIENT = "io.micronaut.oraclecloud.discovery.vault.OracleCloudVaultConfigurationClient";
    private static final String[] AUTH_PROPERTY_KEYS = {
        "oci.auth.strategy",
        "oci.config.path",
        "oci.config.profile",
        "oci.config.enabled",
        "oci.auth.tenant-id",
        "oci.auth.user-id",
        "oci.auth.fingerprint",
        "oci.auth.private-key",
        "oci.auth.pass-phrase",
        "oci.tenant-id",
        "oci.user-id",
        "oci.fingerprint",
        "oci.private-key",
        "oci.private-key-file",
        "oci.passphrase",
        "oci.pass-phrase",
        "oci.signing.enabled",
        "oci.config.session-token",
        "oci.session-token.token",
        "oci.region",
        "oci.config.use-instance-principal",
        "oci.config.instance-principal.enabled",
        "OCI_RESOURCE_PRINCIPAL_VERSION",
        "oci.config.oke-workload-identity.enabled"
    };

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
        normalizeAuthProperties(properties);
        mergeParentAuthProperties(environment, properties);
        normalizeAuthProperties(properties);
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
        builder.configImport(false);
        builder.beanConfigurationsPredicate(vaultBeanConfigurations());
        builder.beansPredicate(vaultBeans());
        if (!singletonOverrides.isEmpty()) {
            builder.singletons(singletonOverrides.values().toArray());
        }
        ApplicationContext applicationContext = builder.build();
        try {
            applicationContext.start();
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
        } catch (RuntimeException | Error e) {
            applicationContext.close();
            throw e;
        }
    }

    private static void mergeParentAuthProperties(Environment environment, Map<String, Object> properties) {
        for (String key : AUTH_PROPERTY_KEYS) {
            if (properties.containsKey(key)) {
                continue;
            }
            environment.getProperty(key, Argument.OBJECT_ARGUMENT)
                .ifPresent(value -> properties.put(key, value));
        }
    }

    private static void normalizeAuthProperties(Map<String, Object> properties) {
        copyIfMissing(properties, "oci.auth.tenant-id", "oci.tenant-id");
        copyIfMissing(properties, "oci.auth.user-id", "oci.user-id");
        copyIfMissing(properties, "oci.auth.fingerprint", "oci.fingerprint");
        copyIfMissing(properties, "oci.auth.private-key", "oci.private-key");
        copyIfMissing(properties, "oci.auth.pass-phrase", "oci.passphrase");
        copyIfMissing(properties, "oci.session-token.token", "oci.config.session-token");
    }

    private static void copyIfMissing(Map<String, Object> properties, String source, String target) {
        if (!properties.containsKey(target) && properties.containsKey(source)) {
            properties.put(target, properties.get(source));
        }
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
            if (beanDefinition.getBeanType().getName().equals(LEGACY_VAULT_CONFIGURATION_CLIENT)) {
                return false;
            }
            if (beanDefinition instanceof BeanDefinitionReference<?> reference) {
                return supportedPackages(reference.getBeanDefinitionName());
            }
            if (beanDefinition instanceof BeanDefinition<?> definition) {
                return definition.getDeclaringType()
                    .map(factory -> supportedPackages(factory.getName()))
                    .orElseGet(() -> supportedPackages(definition.getBeanType().getName()));
            }
            return supportedPackages(beanDefinition.getBeanType().getName());
        };
    }

    private static boolean supportedPackages(String name) {
        return (name.equals("io.micronaut.oraclecloud.core")
                || name.startsWith("io.micronaut.oraclecloud.core.")
                || name.equals("io.micronaut.oraclecloud.clients")
                || name.startsWith("io.micronaut.oraclecloud.clients.")
                || name.equals("io.micronaut.oraclecloud.httpclient")
                || name.startsWith("io.micronaut.oraclecloud.httpclient.")
                || name.equals("io.micronaut.oraclecloud.serde")
                || name.startsWith("io.micronaut.oraclecloud.serde.")
                || name.equals("io.micronaut.oraclecloud.oke.workload.identity")
                || name.startsWith("io.micronaut.oraclecloud.oke.workload.identity.")
                || name.equals("io.micronaut.oraclecloud.discovery.vault")
                || name.startsWith("io.micronaut.oraclecloud.discovery.vault."))
            || name.startsWith("com.oracle.bmc")
            || name.startsWith("io.micronaut.discovery")
            || name.startsWith("io.micronaut.http")
            || name.startsWith("io.micronaut.http.netty")
            || name.startsWith("io.micronaut.jackson")
            || name.startsWith("io.micronaut.json")
            || name.startsWith("io.micronaut.context")
            || name.startsWith("io.micronaut.inject.provider.")
            || name.startsWith("io.micronaut.retry.")
            || name.startsWith("io.micronaut.scheduling")
            || name.startsWith("io.micronaut.core")
            || name.startsWith("io.micronaut.aop")
            || name.startsWith("io.micronaut.runtime")
            || name.startsWith("io.micronaut.serde")
            || name.startsWith("io.micronaut.runtime.converters")
            || name.startsWith("io.micronaut.core.convert")
            || name.startsWith("io.netty")
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
