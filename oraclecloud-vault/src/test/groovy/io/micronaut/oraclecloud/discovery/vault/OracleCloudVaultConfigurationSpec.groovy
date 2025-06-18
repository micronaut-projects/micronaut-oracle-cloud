package io.micronaut.oraclecloud.discovery.vault

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.context.exceptions.NoSuchBeanException
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import spock.lang.Specification

import java.time.Duration

class OracleCloudVaultConfigurationSpec extends Specification {

    void "it parses configuration"() {
        ApplicationContext ctx = ApplicationContext.run([
                'spec.name'                      : 'it parses configuration',
                'micronaut.config-client.enabled': true,
                'oci.vault.config.enabled'       : true,
                'oci.vault.config.retry-attempts': 2,
                'oci.vault.config.retry-delay'   : '200ms',
                'oci.vault.vaults'               : [
                        ['ocid'            : 'ocid1.vault.oc1.phx....',
                         'compartment-ocid': 'ocid1.compartment.oc1....',
                         'includes'        : ['mmm-.*', 'yyy-version1'],
                         'excludes'        : ['zzz-.*', 'yyy-version2']
                        ]
                ]])
        OracleCloudVaultConfiguration config = ctx.getBean(OracleCloudVaultConfiguration)

        expect:
        2 == config.discoveryConfiguration.retryAttempts
        Duration.ofMillis(200) == config.discoveryConfiguration.retryDelay
        1 == config.vaults.size()
        "ocid1.vault.oc1.phx...." == config.vaults[0].ocid
        "ocid1.compartment.oc1...." == config.vaults[0].compartmentOcid
        "mmm-.*" == config.vaults[0].includes[0]
        "yyy-version1" == config.vaults[0].includes[1]
        "zzz-.*" == config.vaults[0].excludes[0]
        "yyy-version2" == config.vaults[0].excludes[1]
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
        ctx.getBean(OracleCloudVaultConfigurationClient)

        then:
        thrown NoSuchBeanException

        when:
        ctx.getBean(OracleCloudVaultConfiguration)

        then:
        thrown NoSuchBeanException

        cleanup:
        ctx.close()
    }

    @Singleton
    @Replaces(OracleCloudVaultConfigurationClient)
    @Requires(property = 'spec.name', value = 'it parses configuration')
    static class MockOracleCloudVaultConfigurationClient extends OracleCloudVaultConfigurationClient {

        MockOracleCloudVaultConfigurationClient(OracleCloudVaultConfiguration vaultConfiguration) {
            super(vaultConfiguration, null, null, null)
        }

        @Override
        Publisher<PropertySource> getPropertySources(Environment environment) {
            return Flux.empty()
        }
    }
}
