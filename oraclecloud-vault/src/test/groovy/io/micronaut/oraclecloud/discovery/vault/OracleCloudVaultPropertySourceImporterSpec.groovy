package io.micronaut.oraclecloud.discovery.vault

import io.micronaut.context.env.PropertySource
import io.micronaut.context.env.PropertySourceImporter
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.core.convert.value.ConvertibleValues
import io.micronaut.core.io.ResourceLoader
import io.micronaut.core.util.ConnectionString
import spock.lang.Specification

import java.util.Optional

class OracleCloudVaultPropertySourceImporterSpec extends Specification {

    void 'it rejects URI declarations missing compartment or vault identifiers'() {
        given:
        def importer = new OracleCloudVaultPropertySourceImporter()

        when:
        importer.newImportDeclaration(ConnectionString.parse('oraclecloud-vault://compartment-only', ConnectionString.ParseMode.HOST))

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('must declare protocol://') || e.message
    }

    void 'it parses URI declarations into vault import configuration'() {
        given:
        def importer = new OracleCloudVaultPropertySourceImporter()

        when:
        def declaration = importer.newImportDeclaration(ConnectionString.parse('oraclecloud-vault://ocid1.compartment.oc1../ocid1.vault.oc1.phx..?includes=DB_.*,API_.*&excludes=DB_LEGACY_PASSWORD&retry-attempts=2&retry-delay=10ms'))
        def importConfiguration = declaration.declaration

        then:
        importConfiguration.compartmentId() == 'ocid1.compartment.oc1..'
        importConfiguration.vaultOcid() == 'ocid1.vault.oc1.phx..'
        importConfiguration.includes() == ['DB_.*', 'API_.*']
        importConfiguration.excludes() == ['DB_LEGACY_PASSWORD']
        importConfiguration.retryAttempts() == 2
        importConfiguration.retryDelay() == '10ms'
        importConfiguration.authProperties().isEmpty()
    }

    void 'it parses provider map declarations into vault import configuration'() {
        given:
        def importer = new OracleCloudVaultPropertySourceImporter()

        when:
        def declaration = importer.newImportDeclaration(ConvertibleValues.of([
            provider: 'oraclecloud-vault',
            'compartment-id': 'ocid1.compartment.oc1..',
            ocid: 'ocid1.vault.oc1.phx..',
            includes: ['DB_.*', 'API_.*'] as String[],
            excludes: ['DB_LEGACY_PASSWORD'] as String[],
            'retry-attempts': 2,
            'retry-delay': '10ms',
            'config-path': '/tmp/oci/config',
            'config-profile': 'ALT'
        ]))
        def importConfiguration = declaration.declaration

        then:
        importConfiguration.compartmentId() == 'ocid1.compartment.oc1..'
        importConfiguration.vaultOcid() == 'ocid1.vault.oc1.phx..'
        importConfiguration.includes() == ['DB_.*', 'API_.*']
        importConfiguration.excludes() == ['DB_LEGACY_PASSWORD']
        importConfiguration.retryAttempts() == 2
        importConfiguration.retryDelay() == '10ms'
        importConfiguration.authProperties().get('oci.config.path') == '/tmp/oci/config'
        importConfiguration.authProperties().get('oci.config.profile') == 'ALT'
    }

    static final class TestImportContext implements PropertySourceImporter.ImportContext<OracleCloudVaultImportConfiguration> {
        private final io.micronaut.context.env.Environment environment
        private final ConnectionString connectionString
        private final OracleCloudVaultImportConfiguration importDeclaration
        private final PropertySource.Origin origin

        TestImportContext(io.micronaut.context.env.Environment environment, ConnectionString connectionString, OracleCloudVaultImportConfiguration importDeclaration, PropertySource.Origin origin) {
            this.environment = environment
            this.connectionString = connectionString
            this.importDeclaration = importDeclaration
            this.origin = origin
        }

        @Override
        io.micronaut.context.env.Environment environment() {
            environment
        }

        @Override
        ConnectionString connectionString() {
            connectionString
        }

        @Override
        OracleCloudVaultImportConfiguration importDeclaration() {
            importDeclaration
        }

        @Override
        PropertySource.Origin parentOrigin() {
            origin
        }

        @Override
        Optional<PropertySource> importPropertySource(ResourceLoader resourceLoader, String resourcePath, String sourceName, PropertySource.Origin origin) {
            throw new UnsupportedOperationException('not used')
        }

        @Override
        Optional<PropertySource> importPropertySource(String content, String sourceName, String extension, PropertySource.Origin origin) {
            throw new UnsupportedOperationException('not used')
        }

        @Override
        Optional<PropertySource> importClasspathPropertySource(String resourcePath, String sourceName, PropertySource.Origin origin, boolean allowMultiple) {
            throw new UnsupportedOperationException('not used')
        }
    }
}
