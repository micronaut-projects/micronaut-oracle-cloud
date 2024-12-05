package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.Region
import com.oracle.bmc.vault.Vaults
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "micronaut.config-client.enabled", value = "false")
@Property(name = "oci.vault.config.enabled", value = "false")
@Property(name = "spec.name", value = "OracleCloudVaultRegionProviderSpec")
class OracleCloudVaultRegionProviderSpec extends Specification {

    @Inject
    Vaults vaults

    void "test region provider"() {
        vaults.getEndpoint() == "https://vaults.%s.oci.oraclecloud.com".formatted(Region.EU_JOVANOVAC_1.regionId)
    }

}
