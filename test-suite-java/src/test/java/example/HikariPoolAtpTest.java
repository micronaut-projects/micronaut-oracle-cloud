package example;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static io.micronaut.core.util.StringUtils.FALSE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "oci.config.path", matches = ".+")
@MicronautTest(startApplication = false)
@Requires(property = "atp.user")
@Requires(property = "atp.pass")
@Requires(property = "atp.ocid")
@Property(name = "micronaut.metrics.export.oraclecloud.enabled", value = FALSE)
class HikariPoolAtpTest {

    @Property(name = "atp.user")
    String userName;

    @Property(name = "atp.pass")
    String password;

    @Property(name = "atp.ocid")
    String atpId;

    @Test
    void testConnectsToDb() throws SQLException {
        try (var ctx = ApplicationContext.run(
            Map.of(
                "datasources.default.ocid", atpId,
                "datasources.default.username", userName,
                "datasources.default.password", password,
                "datasources.default.walletPassword", "FooBar.123",
                "micronaut.metrics.binders.jdbc.enabled", false,
                "micronaut.metrics.export.oraclecloud.enabled", false
            ), Environment.ORACLE_CLOUD
        )) {
            Connection connection = ctx.getBean(DataSource.class).getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM DUAL");
            resultSet.next();
            assertEquals("X", resultSet.getString(1));
        }
    }
}
