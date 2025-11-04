package example.mock

import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Replaces
import io.micronaut.core.annotation.Nullable
import io.micronaut.oraclecloud.core.TenancyIdProvider
import jakarta.inject.Singleton

@Context
@Singleton
@Replaces(TenancyIdProvider::class)
class MockTenancyIdProvider : TenancyIdProvider {
    override fun getTenancyId(): @Nullable String {
        return MockData.tenancyId
    }
}
