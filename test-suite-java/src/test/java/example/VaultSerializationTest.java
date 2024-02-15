package example;

import com.oracle.bmc.vault.model.SecretSummary;
import io.micronaut.context.env.Environment;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest(environments = Environment.ORACLE_CLOUD)
public class VaultSerializationTest {
    @Test
    void testSerialization(JsonMapper jsonMapper) throws IOException {
        var summary = SecretSummary.builder()
            .secretName("foo")
            .description("bar")
            .build();

        String result = jsonMapper.writeValueAsString(summary);
        assertEquals("{\"description\":\"bar\",\"secretName\":\"foo\"}", result);

        SecretSummary secretSummary = jsonMapper.readValue(result, SecretSummary.class);
        assertNotNull(secretSummary);
        assertEquals("foo", secretSummary.getSecretName());
    }
}
