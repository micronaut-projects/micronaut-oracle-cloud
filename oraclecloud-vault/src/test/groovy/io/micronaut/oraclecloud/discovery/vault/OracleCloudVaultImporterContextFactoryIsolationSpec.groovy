package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import com.sun.net.httpserver.HttpServer
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.oraclecloud.monitoring.OracleCloudMeterRegistryFactory
import io.micronaut.oraclecloud.monitoring.sdk.SdkMetricsNettyClientFilter
import spock.lang.Specification
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton

import java.security.KeyPairGenerator
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CopyOnWriteArrayList

class OracleCloudVaultImporterContextFactoryIsolationSpec extends Specification {

    void 'import context resolves OCI clients without importing optional monitoring integration'() {
        given:
        def signatures = new CopyOnWriteArrayList<String>()
        HttpServer server = HttpServer.create(new InetSocketAddress('localhost', 0), 0)
        server.createContext('/') { exchange ->
            signatures.add(exchange.requestHeaders.getFirst('Authorization'))
            String json = exchange.requestURI.path == '/20180608/secrets'
                ? '[{"id":"secret-id","secretName":"DB_PASSWORD","lifecycleState":"ACTIVE"}]'
                : '{"secretBundleContent":{"contentType":"BASE64","content":"bG9hZGVk"}}'
            byte[] body = json.getBytes('UTF-8')
            exchange.responseHeaders.set('Content-Type', 'application/json')
            exchange.sendResponseHeaders(200, body.length)
            exchange.responseBody.withCloseable { it.write(body) }
            exchange.close()
        }
        server.start()
        ApplicationContext parent = ApplicationContext.run(authenticationProperties() + [
            'micronaut.metrics.enabled': 'true',
            'micronaut.metrics.export.oraclecloud.enabled': 'false',
            'oci.config.enabled': 'false'
        ])
        def declaration = OracleCloudVaultImportConfiguration.of(
            'compartment', 'vault', [], [], null, null, [:]
        )
        def factory = new OracleCloudVaultImporterContextFactory()

        when:
        def importerContext = factory.create(parent.environment, declaration, null, null)
        def endpoint = "http://localhost:${server.address.port}".toString()
        importerContext.secretsClient().setEndpoint(endpoint)
        importerContext.vaultsClient().setEndpoint(endpoint)
        def loaded = new OracleCloudVaultPropertySourceLoader(
            importerContext.secretsClient(), importerContext.vaultsClient(),
            importerContext.configuration().discoveryConfiguration
        ).load(importerContext.configuration().vaults)

        then:
        importerContext.secretsClient() instanceof Secrets
        importerContext.vaultsClient() instanceof Vaults
        importerContext.applicationContext().getBean(SimpleAuthenticationDetailsProvider).tenantId == 'parent-tenant'
        !importerContext.applicationContext().containsBean(SdkMetricsNettyClientFilter)
        !importerContext.applicationContext().containsBean(OracleCloudMeterRegistryFactory)
        !importerContext.applicationContext().containsBean(MeterRegistry)
        new String((byte[]) loaded.get('DB_PASSWORD'), 'UTF-8') == 'loaded'
        signatures.size() == 2
        signatures.every { it?.startsWith('Signature ') }

        cleanup:
        importerContext?.close()
        parent?.close()
        server.stop(0)
    }

    void 'import context propagates the OCI auth property names used by the core configuration'() {
        given:
        ApplicationContext parent = ApplicationContext.run([
            'oci.tenant-id': 'parent-tenant',
            'oci.user-id': 'parent-user',
            'oci.fingerprint': 'parent-fingerprint',
            'oci.private-key': 'parent-key',
            'oci.passphrase': 'parent-passphrase',
            'oci.config.enabled': 'false',
            'oci.region': 'us-phoenix-1',
            'oci.signing.enabled': 'false'
        ])
        def declaration = OracleCloudVaultImportConfiguration.of(
            'compartment', 'vault', [], [], null, null,
            ['oci.auth.tenant-id': 'import-tenant']
        )
        def factory = new OracleCloudVaultImporterContextFactory([:])

        when:
        def importerContext = factory.create(
            parent.environment,
            declaration,
            Stub(Secrets),
            Stub(Vaults)
        )

        then:
        importerContext.applicationContext().environment.get('oci.tenant-id', String).get() == 'import-tenant'
        importerContext.applicationContext().environment.get('oci.user-id', String).get() == 'parent-user'
        importerContext.applicationContext().environment.get('oci.fingerprint', String).get() == 'parent-fingerprint'
        importerContext.applicationContext().environment.get('oci.private-key', String).get() == 'parent-key'
        importerContext.applicationContext().environment.get('oci.passphrase', String).get() == 'parent-passphrase'
        importerContext.applicationContext().environment.get('oci.region', String).get() == 'us-phoenix-1'
        importerContext.applicationContext().environment.get('oci.signing.enabled', String).get() == 'false'

        cleanup:
        importerContext?.close()
        parent?.close()
    }

    void 'failed client initialization closes the importer context'() {
        given:
        CloseProbe.closed.set(false)
        ApplicationContext parent = ApplicationContext.run(authenticationProperties())
        def declaration = OracleCloudVaultImportConfiguration.of(
            'compartment', 'vault', [], [], null, null, ['spec.name': 'failing-vault-import']
        )

        when:
        new OracleCloudVaultImporterContextFactory().create(parent.environment, declaration, null, null)

        then:
        def error = thrown(RuntimeException)
        error.message.contains('deliberate client initialization failure')
        CloseProbe.closed.get()

        cleanup:
        parent?.close()
    }

    private static Map<String, Object> authenticationProperties() {
        def generator = KeyPairGenerator.getInstance('RSA')
        generator.initialize(2048)
        def encoded = Base64.mimeEncoder.encodeToString(generator.generateKeyPair().private.encoded)
        [
            'oci.tenant-id': 'parent-tenant',
            'oci.user-id': 'parent-user',
            'oci.fingerprint': 'parent-fingerprint',
            'oci.private-key': "-----BEGIN PRIVATE KEY-----\n${encoded}\n-----END PRIVATE KEY-----\n".toString(),
            'oci.config.enabled': false,
            'oci.region': 'us-phoenix-1',
            'micronaut.metrics.export.oraclecloud.enabled': false
        ]
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'failing-vault-import')
    static class CloseProbe {
        static final AtomicBoolean closed = new AtomicBoolean()

        @PreDestroy
        void close() {
            closed.set(true)
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'failing-vault-import')
    static class FailingClientListener implements BeanCreatedEventListener<Secrets> {
        FailingClientListener(CloseProbe probe) {
        }

        @Override
        Secrets onCreated(BeanCreatedEvent<Secrets> event) {
            throw new IllegalStateException('deliberate client initialization failure')
        }
    }
}
