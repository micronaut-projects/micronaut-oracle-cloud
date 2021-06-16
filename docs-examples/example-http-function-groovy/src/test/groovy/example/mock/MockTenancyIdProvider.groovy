package example.mock

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Replaces
import io.micronaut.oraclecloud.core.TenancyIdProvider

import javax.inject.Singleton

@CompileStatic
@Context
@Singleton
@Replaces(TenancyIdProvider)
class MockTenancyIdProvider implements TenancyIdProvider {
    String tenancyId = MockData.tenancyId
}
