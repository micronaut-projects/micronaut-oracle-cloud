package io.micronaut.oraclecloud.atp.jdbc.hikari

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration
import io.micronaut.context.BeanLocator
import io.micronaut.context.event.BeanInitializingEvent
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.oraclecloud.atp.jdbc.AutonomousDatabaseConfiguration
import io.micronaut.oraclecloud.atp.jdbc.OracleWalletArchiveProvider
import io.micronaut.oraclecloud.atp.wallet.datasource.CanConfigureOracleDataSource
import oracle.jdbc.datasource.impl.OracleDataSource
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet
import java.util.Optional

@Requires({ System.getenv("ATP_USER") && System.getenv("ATP_PASS") && System.getenv("ATP_OCID") })
class HikariPoolConfigurationListenerSpec extends Specification {

    @Shared
    String userName = System.getenv("ATP_USER")

    @Shared
    String password = System.getenv("ATP_PASS")

    @Shared
    String atpId = System.getenv("ATP_OCID")

    void "test it connects to database"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "datasources.default.ocid"          : atpId,
                "datasources.default.username"      : userName,
                "datasources.default.password"      : password,
                "datasources.default.walletPassword": "FooBar.123"
        ], Environment.ORACLE_CLOUD)

        when:
        DataSource dataSource = context.getBean(DataSource)

        then:
        Connection connection = dataSource.getConnection()
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM DUAL")
        resultSet.next()
        resultSet.getString(1) == "X"

        cleanup:
        context.close()
    }

    void "test it skips datasource without ocid field"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "datasources.default.url"     : "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "datasources.default.username": userName,
                "datasources.default.password": password,
        ], Environment.ORACLE_CLOUD)

        when:
        DataSource dataSource = context.getBean(DataSource)

        then:
        Connection connection = dataSource.getConnection()
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT 1")
        resultSet.next()
        resultSet.getString(1) == "1"

        cleanup:
        context.close()
    }

    void "test it configures datasource when wallet password is omitted"() {
        given:
        BeanLocator beanLocator = Mock()
        OracleWalletArchiveProvider walletArchiveProvider = Mock()
        CanConfigureOracleDataSource walletArchive = Stub() {
            configure(_ as OracleDataSource) >> { OracleDataSource dataSource ->
                dataSource.setURL("jdbc:oracle:thin:@db_high?TNS_ADMIN=/tmp/wallet")
            }
        }
        HikariPoolConfigurationListener listener = new HikariPoolConfigurationListener(walletArchiveProvider, beanLocator)
        DatasourceConfiguration datasourceConfiguration = new DatasourceConfiguration("default")
        AutonomousDatabaseConfiguration autonomousDatabaseConfiguration = new AutonomousDatabaseConfiguration()
        autonomousDatabaseConfiguration.setOcid("ocid1.autonomousdatabase.oc1..example")
        BeanInitializingEvent<DatasourceConfiguration> event = Stub() {
            getBean() >> datasourceConfiguration
        }

        beanLocator.findBean(AutonomousDatabaseConfiguration, _) >> Optional.of(autonomousDatabaseConfiguration)

        when:
        listener.onInitialized(event)

        then:
        1 * walletArchiveProvider.loadWalletArchive(autonomousDatabaseConfiguration) >> walletArchive
        datasourceConfiguration.getUrl() == "jdbc:oracle:thin:@db_high?TNS_ADMIN=/tmp/wallet"
        datasourceConfiguration.getDriverClassName() == HikariPoolConfigurationListener.ORACLE_JDBC_ORACLE_DRIVER
    }
}
