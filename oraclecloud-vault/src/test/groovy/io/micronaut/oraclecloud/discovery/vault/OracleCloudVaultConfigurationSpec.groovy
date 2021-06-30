package io.micronaut.oraclecloud.discovery.vault

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.NoSuchBeanException
import spock.lang.Specification

class OracleCloudVaultConfigurationSpec extends Specification {

    void "it parses configuration"() {
        ApplicationContext ctx = ApplicationContext.run([
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled'       : true,
                'oci.vault.vaults'               : [
                        ['ocid'            : 'ocid1.vault.oc1.phx....',
                         'compartment-ocid': 'ocid1.compartment.oc1....']
                ]])
        OracleCloudVaultConfiguration config = ctx.getBean(OracleCloudVaultConfiguration.class)

        expect:
        1 == config.getVaults().size()
        "ocid1.vault.oc1.phx...." == config.getVaults().get(0).getOcid()
        "ocid1.compartment.oc1...." == config.getVaults().get(0).getCompartmentOcid()
        config.discoveryConfiguration.enabled

        cleanup:
        ctx.close()
    }

    void "it is missing vault configuration client bean when disabled"() {
        given:
        ApplicationContext ctx = ApplicationContext.run([
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled'       : false,
                'oci.vault.vaults'               : [
                        ['ocid'            : 'ocid1.vault.oc1.phx....',
                         'compartment-ocid': 'ocid1.compartment.oc1....']
                ]])

        when:
        ctx.getBean(OracleCloudVaultConfigurationClient.class)

        then:
        thrown NoSuchBeanException

        when:
        ctx.getBean(OracleCloudVaultConfiguration.class)

        then:
        thrown NoSuchBeanException

        cleanup:
        ctx.close()
    }
}
