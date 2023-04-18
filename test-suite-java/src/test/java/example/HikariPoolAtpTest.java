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
import java.util.HashMap;


@MicronautTest
@Requires(property = "atp.user")
@Requires(property = "atp.pass")
@Requires(property = "atp.ocid")
public class HikariPoolAtpTest {

    @Property(name = "atp.user")
    String userName;

    @Property(name = "atp.pass")
    String password;

    @Property(name = "atp.ocid")
    String atpId;

    @Test
    void testConnectsToDb() throws SQLException {

        ApplicationContext context = ApplicationContext.run(new HashMap<>(){{
            put("datasources.default.ocid", atpId);
            put("datasources.default.username", userName);
            put("datasources.default.password", password);
            put("datasources.default.walletPassword",  "FooBar.123");
        }}, Environment.ORACLE_CLOUD);

        DataSource dataSource = context.getBean(DataSource.class);

        Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM DUAL");
        resultSet.next();
        Assertions.assertEquals(resultSet.getString(1), "X");

    }


}
