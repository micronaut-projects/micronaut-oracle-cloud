package io.micronaut.oraclecloud.atp.jdbc.upc

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet

@Requires({ System.getenv("ATP_USER") && System.getenv("ATP_PASS") && System.getenv("ATP_OCID") })
class UcpPoolConfigurationListenerSpec extends Specification {

    @Shared
    String userName = "${System.getenv("ATP_USER")}"

    @Shared
    String password = "${System.getenv("ATP_PASS")}"

    @Shared
    String atpId = "${System.getenv("ATP_OCID")}"

    def "test it connects to database"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "datasources.default.ocid"          : atpId,
                "datasources.default.username"      : userName,
                "datasources.default.password"      : password,
                "datasources.default.walletPassword": "FooBar.123"
        ], Environment.ORACLE_CLOUD)

        when:
        PoolDataSource poolDataSource = context.getBean(PoolDataSource)

        then:
        Connection connection = poolDataSource.getConnection()
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM DUAL")
        resultSet.next()
        resultSet.getString(1) == "X"

        cleanup:
        context.close()
    }

    def "test it skips datasource without ocid field"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "datasources.default.url"                       : "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "datasources.default.username"                  : userName,
                "datasources.default.password"                  : password,
                "datasources.default.connectionFactoryClassName": "oracle.jdbc.pool.OracleDataSource",
                "datasources.default.driverClassName"           : "org.h2.Driver",
                "datasources.default.maxPoolSize"               : 1,
                "datasources.default.minPoolSize"               : 1
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
}
