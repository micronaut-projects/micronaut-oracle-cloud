package io.micronaut.oraclecloud.discovery.vault

import io.micronaut.context.ApplicationContext
import java.nio.charset.StandardCharsets
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

/**
 * This spec prerequisite is to have an existing vault with one secret configured.
 */
@Requires({ System.getenv("VAULT_OCID") && System.getenv("VAULT_SECRETS_COMPARTMENT_OCID") && System.getenv("VAULT_SECRET_NAME") && System.getenv("VAULT_SECRET_VALUE") })
class OracleCloudVaultPropertySourceImporterLiveSpec extends Specification {

    @Shared
    String vaultOcid = System.getenv("VAULT_OCID")

    @Shared
    String compartmentOcid = System.getenv("VAULT_SECRETS_COMPARTMENT_OCID")

    @Shared
    String secretName = System.getenv("VAULT_SECRET_NAME")

    @Shared
    String secretValue = System.getenv("VAULT_SECRET_VALUE")

    @Shared
    String region = System.getenv("OCI_REGION")

    @Shared
    boolean configFileEnabled = Boolean.parseBoolean(System.getenv("OCI_CONFIG_ENABLED"))

    @Shared
    String configPath = System.getenv("OCI_CONFIG_PATH")

    @Shared
    String configProfile = System.getenv("OCI_CONFIG_PROFILE")

    void "importer loads secret from real vault using documented connection string syntax"() {
        given:
        String importValue = buildImportValue()
        ApplicationContext ctx = ApplicationContext.run([
            'micronaut.config.import[0]': importValue,
            'micronaut.metrics.export.oraclecloud.enabled': false,
            'micronaut.config-client.enabled': false,
            'oci.vault.config.enabled': true,
        ])

        expect:
        ctx.containsProperty(secretName)
        ctx.getProperty(secretName, String).orElse(null) == secretValue
        !ctx.containsProperty('definitely-not-a-real-vault-secret')

        cleanup:
        ctx.close()
    }

    private String buildImportValue() {
        List<String> options = []
        if (configFileEnabled) {
            if (configPath) {
                options << "config-path=${configPath}"
            }
            if (configProfile) {
                options << "config-profile=${encode(configProfile)}"
            }
        } else {
            options << "use-instance-principal=true"
        }
        if (region) {
            options << "region=${encode(region)}"
        }
        String query = options.isEmpty() ? "" : "?${options.join('&')}"
        "oraclecloud-vault://${compartmentOcid}/${vaultOcid}${query}"
    }

    private static String encode(String value) {
        URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

}
