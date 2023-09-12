package io.micronaut.oraclecloud.serde

import com.oracle.bmc.vault.model.Base64SecretContentDetails
import com.oracle.bmc.vault.model.SecretContentDetails
import com.oracle.bmc.vault.model.SecretExpiryRule
import com.oracle.bmc.vault.model.SecretReuseRule
import com.oracle.bmc.vault.model.SecretRule
import io.micronaut.runtime.server.EmbeddedServer

import java.time.Instant

class VaultSerdeSpec extends SerdeSpecBase {

    void "SecretContent serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var body = echoTest(embeddedServer, content)

        then:
        serialized == body

        cleanup:
        embeddedServer.close()

        where:
        content | serialized
        Base64SecretContentDetails.builder().content("secret content").build()
                | '{"contentType":"BASE64","content":"secret content"}'
        Base64SecretContentDetails.builder().content("content").name("name").stage(SecretContentDetails.Stage.Current).build()
                | '{"contentType":"BASE64","name":"name","stage":"CURRENT","content":"content"}'
    }

    void "SecretContent deserialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        def content = echoTest(embeddedServer, body, SecretContentDetails)

        then:
        equalsIgnoreExplicitlySet(expected, content)

        cleanup:
        embeddedServer.close()

        where:
        expected   | body
        Base64SecretContentDetails.builder().content("secret content").build()
                   | '{"contentType":"BASE64","content":"secret content"}'
        Base64SecretContentDetails.builder().content("content").name("name").stage(SecretContentDetails.Stage.Current).build()
                   | '{"contentType":"BASE64","name":"name","stage":"CURRENT","content":"content"}'
    }

    void "SecretRule serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var body = echoTest(embeddedServer, content)

        then:
        serialized.replace("'", '"') == body

        cleanup:
        embeddedServer.close()

        where:
        content | serialized
        SecretExpiryRule.builder().timeOfAbsoluteExpiry(Date.from(Instant.parse("2000-02-03T12:00:00Z"))).build()
                | '{"ruleType":"SECRET_EXPIRY_RULE","timeOfAbsoluteExpiry":"2000-02-03T12:00:00Z"}'
        SecretExpiryRule.builder().timeOfAbsoluteExpiry(Date.from(Instant.parse("2000-02-03T12:00:00Z"))).isSecretContentRetrievalBlockedOnExpiry(true).build()
                | '{"ruleType":"SECRET_EXPIRY_RULE","timeOfAbsoluteExpiry":"2000-02-03T12:00:00Z","isSecretContentRetrievalBlockedOnExpiry":true}'
        SecretReuseRule.builder().isEnforcedOnDeletedSecretVersions(true).build()
                | '{"ruleType":"SECRET_REUSE_RULE","isEnforcedOnDeletedSecretVersions":true}'
        SecretReuseRule.builder().isEnforcedOnDeletedSecretVersions(false).build()
                | '{"ruleType":"SECRET_REUSE_RULE","isEnforcedOnDeletedSecretVersions":false}'
    }

    void "SecretRule deserialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        def content = echoTest(embeddedServer, body.replace("'", '"'), SecretRule)

        then:
        equalsIgnoreExplicitlySet(expected, content)

        cleanup:
        embeddedServer.close()

        where:
        expected  | body
        SecretExpiryRule.builder().timeOfAbsoluteExpiry(Date.from(Instant.parse("2000-02-03T12:00:00Z"))).build()
                | '{"ruleType":"SECRET_EXPIRY_RULE","timeOfAbsoluteExpiry":"2000-02-03T12:00:00Z"}'
        SecretExpiryRule.builder().timeOfAbsoluteExpiry(Date.from(Instant.parse("2000-02-03T12:00:00Z"))).isSecretContentRetrievalBlockedOnExpiry(true).build()
                | '{"ruleType":"SECRET_EXPIRY_RULE","timeOfAbsoluteExpiry":"2000-02-03T12:00:00Z","isSecretContentRetrievalBlockedOnExpiry":true}'
        SecretReuseRule.builder().isEnforcedOnDeletedSecretVersions(true).build()
                | '{"ruleType":"SECRET_REUSE_RULE","isEnforcedOnDeletedSecretVersions":true}'
        SecretReuseRule.builder().isEnforcedOnDeletedSecretVersions(false).build()
                | '{"ruleType":"SECRET_REUSE_RULE","isEnforcedOnDeletedSecretVersions":false}'
    }
}
