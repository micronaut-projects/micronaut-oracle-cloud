package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.oraclecloud.core.TenancyIdProvider;
import jakarta.inject.Singleton;

@Context
@Singleton
@Replaces(TenancyIdProvider.class)
public class MockTenancyIdProvider implements TenancyIdProvider {

    @Nullable
    @Override
    public String getTenancyId() {
        return MockData.tenancyId;
    }
}
