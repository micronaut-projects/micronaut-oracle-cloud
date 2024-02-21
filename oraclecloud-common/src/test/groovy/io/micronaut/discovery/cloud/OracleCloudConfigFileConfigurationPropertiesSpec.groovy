package io.micronaut.discovery.cloud

import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider
import io.micronaut.context.ApplicationContext
import io.micronaut.oraclecloud.core.OracleCloudConfigFileConfigurationProperties
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static io.micronaut.oraclecloud.core.OracleCloudCoreFactory.ORACLE_CLOUD_CONFIG_PATH


class OracleCloudConfigFileConfigurationPropertiesSpec extends Specification {

    @Shared
    @TempDir
    Path testPath

    @Shared
    File ociConfig

    void setupSpec() {
        ociConfig = testPath.resolve("config").toFile()
        ociConfig.text = """
[DEFAULT]
user=ocid1.user.oc1..aaaaaaaaxxxx
fingerprint=xx:xx:xx:xx:xx:xx:xx
key_file=${testPath}/oci_api_key.pem
tenancy=ocid1.tenancy.oc1..aaaaaaaaxxxx
region=us-ashburn-1
pass_phrase=xxxxx
security_token_file=${testPath}/oci_api_key.pem
"""
        testPath.resolve("oci_api_key.pem").toFile().text = new File("src/test/resources/oci_api_key.pem").text

    }

    void 'it is enabled by default'() {
        given:
        def ctx = ApplicationContext.run([
                (ORACLE_CLOUD_CONFIG_PATH): ociConfig.absolutePath
        ])

        expect:
        ctx.containsBean(ConfigFileAuthenticationDetailsProvider)
        !ctx.containsBean(SessionTokenAuthenticationDetailsProvider)

        cleanup:
        ctx.close()
    }

    void 'it can be disabled even if config file exists'() {
        given:
        def ctx = ApplicationContext.run([
                (ORACLE_CLOUD_CONFIG_PATH): ociConfig.absolutePath,
                (OracleCloudConfigFileConfigurationProperties.PREFIX + ".enabled"): false
        ])

        expect:
        !ctx.containsBean(ConfigFileAuthenticationDetailsProvider)
        !ctx.containsBean(SessionTokenAuthenticationDetailsProvider)

        cleanup:
        ctx.close()
    }

    void 'it can enable session token authentication'() {
        given:
        def ctx = ApplicationContext.run([
                (ORACLE_CLOUD_CONFIG_PATH): ociConfig.absolutePath,
                (OracleCloudConfigFileConfigurationProperties.PREFIX + ".session-token"): true
        ])

        expect:
        !ctx.containsBean(ConfigFileAuthenticationDetailsProvider)
        ctx.containsBean(SessionTokenAuthenticationDetailsProvider)

        cleanup:
        ctx.close()
    }
}
