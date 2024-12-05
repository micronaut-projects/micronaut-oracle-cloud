package io.micronaut.oraclecloud.discovery.vault;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.RegionProvider;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;


@Singleton
@Primary
@BootstrapContextCompatible
@Replaces(RegionProvider.class)
@Bean(typed = RegionProvider.class)
@Requires(property = "spec.name", value = "OracleCloudVaultRegionProviderSpec")
public class RegionProviderTest implements RegionProvider {
    @Override
    public Region getRegion() {
        return Region.EU_JOVANOVAC_1;
    }
}
