package io.micronaut.oraclecloud.discovery.vault

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class VaultConfigurationTest extends Specification {

    void testConfig() {
        ApplicationContext ctx = ApplicationContext.run([
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled': true,
                'oci.vault.vaults': [
                        ['ocid': 'ocid1.vault.oc1.phx....',
                         'compartment-ocid': 'ocid1.compartment.oc1....']
                ]]);
        OracleCloudVaultClientConfiguration config = ctx.getBean(OracleCloudVaultClientConfiguration.class)
        def client = ctx.getBean(OracleCloudVaultConfigurationClient.class)

        expect:
        1 == config.getVaults().size()
        "ocid1.vault.oc1.phx...." == config.getVaults().get(0).getOcid()
        "ocid1.compartment.oc1...." == config.getVaults().get(0).getCompartmentOcid()
        client != null

        cleanup:
        ctx.close()
    }
}


