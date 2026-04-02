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

import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceImporter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.util.ConnectionString;

import java.util.Optional;

/**
 * Imports Oracle Cloud Vault distributed configuration via {@code micronaut.config.import}.
 *
 * @author graemerocher
 * @since 6.0.0
 */
@Internal
public final class OracleCloudVaultPropertySourceImporter implements PropertySourceImporter<OracleCloudVaultImportConfiguration> {

    static final String PROVIDER = "oraclecloud-vault";
    private final OracleCloudVaultImporterContextFactory contextFactory = new OracleCloudVaultImporterContextFactory();
    private final OracleCloudVaultImportConfigurationBinder binder = new OracleCloudVaultImportConfigurationBinder();
    private OracleCloudVaultImporterContextFactory.OracleCloudVaultImporterContext importerContext;

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public OracleCloudVaultImportConfiguration newImportDeclaration(ConnectionString connectionString) {
        return binder.bind(connectionString);
    }

    @Override
    public OracleCloudVaultImportConfiguration newImportDeclaration(ConvertibleValues<Object> values) {
        return binder.bind(values);
    }

    @Override
    public Optional<PropertySource> importPropertySource(ImportContext<OracleCloudVaultImportConfiguration> context) {
        OracleCloudVaultImportConfiguration importConfiguration = context.importDeclaration();
        OracleCloudVaultImporterContextFactory.OracleCloudVaultImporterContext importerContext = getOrCreateImporterContext(context, importConfiguration);
        OracleCloudVaultConfiguration configuration = importerContext.configuration();
        if (configuration.getVaults().isEmpty()) {
            return Optional.empty();
        }
        OracleCloudVaultPropertySourceLoader loader = new OracleCloudVaultPropertySourceLoader(
            importerContext.secretsClient(),
            importerContext.vaultsClient(),
            configuration.getDiscoveryConfiguration()
        );
        return Optional.of(loader.load(configuration.getVaults()));
    }

    @Override
    public void close() {
        if (importerContext != null) {
            importerContext.close();
            importerContext = null;
        }
    }

    private OracleCloudVaultImporterContextFactory.OracleCloudVaultImporterContext getOrCreateImporterContext(ImportContext<OracleCloudVaultImportConfiguration> context,
                                                                                                                OracleCloudVaultImportConfiguration importConfiguration) {
        if (importerContext == null) {
            importerContext = contextFactory.create(
                context.environment(),
                importConfiguration,
                null,
                null);
        }
        return importerContext;
    }
}
