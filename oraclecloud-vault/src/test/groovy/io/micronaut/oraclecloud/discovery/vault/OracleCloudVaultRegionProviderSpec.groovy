package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.Region
import com.oracle.bmc.auth.RegionProvider
import com.oracle.bmc.vault.Vaults
import com.oracle.bmc.vault.VaultsClient
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

@MicronautTest
@Property(name = "micronaut.config-client.enabled", value = "false")
@Property(name = "oci.vault.config.enabled", value = "false")
@Property(name = "spec.name", value = "OracleCloudVaultRegionProviderSpec")
class OracleCloudVaultRegionProviderSpec extends Specification {

    @Inject
    Vaults vaults

    void "it test region provider" () {
        when:
        String endpoint = vaults.getEndpoint()

        then:
        endpoint == Region.EU_JOVANOVAC_1.getEndpoint(VaultsClient.SERVICE).get()
    }


    @Singleton
    @BootstrapContextCompatible
    @Primary
    @Replaces(RegionProvider.class)
    @Requires(property = "spec.name", value = "OracleCloudVaultRegionProviderSpec")
    static class RegionProviderReplacement implements RegionProvider {

        @Override
        Region getRegion() {
            return Region.EU_JOVANOVAC_1;
        }
    }

}
