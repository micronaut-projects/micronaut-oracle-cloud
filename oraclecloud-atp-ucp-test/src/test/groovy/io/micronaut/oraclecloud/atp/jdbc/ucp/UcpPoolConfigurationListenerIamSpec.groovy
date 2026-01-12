package io.micronaut.oraclecloud.atp.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.oraclecloud.atp.jdbc.AutonomousDatabaseConfiguration
import io.micronaut.oraclecloud.atp.jdbc.OracleWalletArchiveProvider
import io.micronaut.oraclecloud.atp.jdbc.iam.IamDbTokenProvider
import io.micronaut.oraclecloud.atp.wallet.datasource.CanConfigureOracleDataSource
import io.micronaut.oraclecloud.atp.wallet.datasource.OracleDataSourceAttributes
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Requires
import spock.lang.Specification

/**
 * Verifies IAM mode wiring for UCP:
 * - Wallet/configuration path is executed (simulated by stub CanConfigureOracleDataSource)
 * - Password is not required; token connection properties are merged into PoolDataSource
 * This test does not require a real ADB or OCI; it registers a stub OracleWalletArchiveProvider.
 */
@Requires({ System.getenv("OCI_ATP_IAM_TEST") })
class UcpPoolConfigurationListenerIamSpec extends Specification {

    void "IAM mode applies token properties to PoolDataSource (UCP)"() {
        given:
        Map<String, Object> cfg = [
                "datasources.default.ocid"                                  : "ocid1.autonomousdatabase.oc1.test",
                "datasources.default.walletPassword"                        : "Test1234!",
                "datasources.default.oraclecloud.atp.auth"                  : AutonomousDatabaseConfiguration.AuthMode.IAM.name(),
                "datasources.default.oraclecloud.atp.iamProviderQualifier"  : "iamTest"
        ]
        def ctx = ApplicationContext.builder(cfg + [
                "datasources.default.enabled": false // avoid creating and starting UCP pool bean
        ], Environment.ORACLE_CLOUD)
                .eagerInitSingletons(false) // avoid eager pool start
                .build()

        // Stub token provider returning a simple property
        def tokenProvider = new IamDbTokenProvider() {
            @Override
            Properties tokenConnectionProperties(String dataSourceName, String serviceAlias) {
                Properties p = new Properties()
                p.setProperty("test.token", "abc123")
                return p
            }
        }
        ctx.registerSingleton(IamDbTokenProvider, tokenProvider, Qualifiers.byName("iamTest"))

        // Stub wallet archive provider that configures URL, user and password via attributes
        def stubWalletArchive = new CanConfigureOracleDataSource() {
            @Override
            OracleDataSourceAttributes configure(OracleDataSourceAttributes dataSource) {
                dataSource.url("jdbc:oracle:thin:@//localhost:1521/FOO_high")
                dataSource.user("scott")
                dataSource.password("tiger".chars)
                return dataSource
            }
        }
        def providerMock = Mock(OracleWalletArchiveProvider)
        providerMock.loadWalletArchive(_ as AutonomousDatabaseConfiguration) >> stubWalletArchive
        ctx.registerSingleton(OracleWalletArchiveProvider, providerMock)

        when:
        ctx.start()
        def dsCfg = ctx.getBean(io.micronaut.configuration.jdbc.ucp.DatasourceConfiguration, Qualifiers.byName("default"))
        PoolDataSource pds = dsCfg.getPoolDataSource()

        then:
        pds != null
        // URL is held on dsCfg, but properties are applied on PoolDataSource
        dsCfg.getUrl() == "jdbc:oracle:thin:@//localhost:1521/FOO_high"
        pds.getConnectionProperties() != null
        pds.getConnectionProperties().getProperty("test.token") == "abc123"

        cleanup:
        ctx.close()
    }
}
