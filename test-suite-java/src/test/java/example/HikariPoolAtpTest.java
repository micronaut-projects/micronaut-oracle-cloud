package example;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@MicronautTest(startApplication = false)
@Requires(property = "atp.user")
@Requires(property = "atp.pass")
@Requires(property = "atp.ocid")
class HikariPoolAtpTest {

    @Property(name = "atp.user")
    String userName;

    @Property(name = "atp.pass")
    String password;

    @Property(name = "atp.ocid")
    String atpId;

    @Test
    void testConnectsToDb() throws SQLException {
        ApplicationContext context = ApplicationContext.run(
            Map.of(
                "datasources.default.ocid", atpId,
                "datasources.default.username", userName,
                "datasources.default.password", password,
                "datasources.default.walletPassword",  "FooBar.123",
                "micronaut.metrics.export.oraclecloud.enabled", false
            ), Environment.ORACLE_CLOUD
        );

        DataSource dataSource = context.getBean(DataSource.class);

        Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM DUAL");
        resultSet.next();
        Assertions.assertEquals("X", resultSet.getString(1));

    }
}
