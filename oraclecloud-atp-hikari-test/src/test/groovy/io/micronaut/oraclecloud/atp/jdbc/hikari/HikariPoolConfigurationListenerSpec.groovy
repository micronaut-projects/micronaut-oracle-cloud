package io.micronaut.oraclecloud.atp.jdbc.hikari


import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet

class HikariPoolConfigurationListenerSpec extends Specification {

    @Shared
    String userName = "mnuser0"

    @Shared
    String password = "0xSc6eNP2ByomnlcHpqN1"

    @Shared
    String serviceAlias = "PROJECTOR8534_LOW"

    @Shared
    String atpId = "ocid1.autonomousdatabase.oc1.phx.abyhqljtjfv7x4przibqqapvr4snlafaw5k3w6gsk5wjotjnqnmdgi6an2hq"

    def "test it connects to database"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "datasources.default.ocid"           : atpId,
                "datasources.default.username"       : "mnuser0",
                "datasources.default.password"       : password,
                "datasources.default.serviceAlias"   : serviceAlias,
                "datasources.default.walletPassword" : "Vajco.123"
        ], Environment.ORACLE_CLOUD)


        when:
        DataSource dataSource = context.getBean(DataSource.class)

        then:
        Connection connection = dataSource.getConnection()
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM DUAL")
        resultSet.next()
        resultSet.getString(1) == "X"
    }
}
