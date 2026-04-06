package io.micronaut.oraclecloud.discovery.vault

import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.oraclecloud.core.TenancyIdProvider
import jakarta.inject.Singleton

@BootstrapContextCompatible
@Context
@Singleton
@Replaces(TenancyIdProvider)
@Requires(property = "vault.test-mocks.enabled", value = "true")
class MockTenancyIdProvider implements TenancyIdProvider {

    @Override
    String getTenancyId() {
        'test-tenancy'
    }
}
