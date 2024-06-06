package example;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.oraclecloud.core.TenancyIdProvider;
import jakarta.inject.Singleton;

@Context
@Singleton
@Replaces(TenancyIdProvider.class)
public class MockTenancyIdProvider implements TenancyIdProvider {

    public static String tenancyId = "test-tenancyId";


    @Nullable
    @Override
    public String getTenancyId() {
        return tenancyId;
    }
}
