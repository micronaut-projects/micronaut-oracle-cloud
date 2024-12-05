package example;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.RegionProvider;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

@Primary
@Replaces(RegionProvider.class)
@BootstrapContextCompatible
@Singleton
@Requires(property = "spec.name", value = "VaultRegionProviderTest")
public class VaultRegionProviderMock implements RegionProvider {
    @Override
    public Region getRegion() {
        return Region.EU_JOVANOVAC_1;
    }
}
