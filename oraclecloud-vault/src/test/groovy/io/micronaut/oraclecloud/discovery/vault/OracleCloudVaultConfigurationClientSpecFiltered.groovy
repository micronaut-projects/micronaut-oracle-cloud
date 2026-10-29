package io.micronaut.oraclecloud.discovery.vault


import com.oracle.bmc.secrets.Secrets
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContextBuilder
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.env.PropertySource
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import spock.lang.Specification

/**
 * Test with a mock list of secrets to validate that the vault bootstrap config client correctly
 * filters out excluded secrets and includes only the specified included secrets.
 *
 * @author schangj09
 */
@MicronautTest(contextBuilder = MyContextBuilder)
class OracleCloudVaultConfigurationClientSpecFiltered extends Specification {
    public static final String VAULT_OCID = "vault1"
    public static final String COMPARTMENT_OCID = "compartment1"

    static def secretsList = [
            new MockVaultSecrets.Secret(name: "xxx-v1", id: "x1", value: "xv1"),
            new MockVaultSecrets.Secret(name: "xxx-v2", id: "x2", value: "xv2"),
            new MockVaultSecrets.Secret(name: "yyy-v1", id: "y1", value: "yv1"),
            new MockVaultSecrets.Secret(name: "yyy-v2", id: "y2", value: "yv2"),
            new MockVaultSecrets.Secret(name: "zzz-v1", id: "z1", value: "zv1"),
            new MockVaultSecrets.Secret(name: "zzz-v2", id: "z2", value: "zv2"),
            new MockVaultSecrets.Secret(name: "mmm-v1", id: "m1", value: "mv1"),
            new MockVaultSecrets.Secret(name: "mmm-v2", id: "m2", value: "mv2"),
    ]

    private static MockVaultSecrets vaultSecrets = new MockVaultSecrets(secretsList)

    @Inject
    ApplicationContext context

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Secrets secrets() {
        vaultSecrets.secretsClient
    }

    @MockBean
    @BootstrapContextCompatible
    @Primary
    static Vaults vaults() {
        vaultSecrets.vaultsClient
    }


    void "it loads filtered secrets from the vault"() {
        given:
        ApplicationContext ctx = context
        ctx.isRunning()

        when:
        def client = ctx.getBean(OracleCloudVaultConfigurationClient.class)
        PropertySource propertySource = Flux.from(client.getPropertySources(null)).blockFirst()

        then:
        assert !propertySource.isEmpty()
        def expectedSecrets = [
                new MockVaultSecrets.Secret(name: "yyy-v1", id: "y1", value: "yv1"),
                new MockVaultSecrets.Secret(name: "mmm-v2", id: "m2", value: "mv2"),
        ]
        secretsList.forEach { secret ->
            if (expectedSecrets.contains(secret)) {
                assert propertySource.get(secret.name) instanceof byte[]
                assert new String((byte[]) propertySource.get(secret.name)) == secret.value
            } else {
                assert !propertySource.contains(secret.name)
            }
        }
    }

    static class MyContextBuilder extends DefaultApplicationContextBuilder {
        MyContextBuilder() {
            bootstrapEnvironment(true)
            properties([
                    'micronaut.config-client.enabled': true,
                    'oci.vault.config.enabled'       : true,
                    'oci.vault.config.retry-attempts': 2,
                    'oci.vault.config.retry-delay'   : '50ms',
                    'oci.vault.vaults'               : [
                            ['ocid'            : VAULT_OCID,
                             'compartment-ocid': COMPARTMENT_OCID,
                             'includes'        : ['mmm-.*', 'yyy-v1'],
                             'excludes'        : ['mmm-v1']]
                    ]])
        }
    }
}


