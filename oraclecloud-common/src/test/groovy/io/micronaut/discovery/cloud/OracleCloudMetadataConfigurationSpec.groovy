package io.micronaut.discovery.cloud

import io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataConfiguration
import spock.lang.Specification

class OracleCloudMetadataConfigurationSpec extends Specification {

    void 'it configures the default URL depending on the version'() {
        given:
        def cfg = new OracleCloudMetadataConfiguration()

        expect: "v1 is enabled by default"
        cfg.v1Enabled
        !cfg.v2Enabled

        and: "default URLs are set"
        cfg.url == OracleCloudMetadataConfiguration.DEFAULT_URL
        cfg.vnicUrl == OracleCloudMetadataConfiguration.DEFAULT_VNIC_URL

        and: "deprecated values return the url"
        cfg.metadataUrl == cfg.url
        cfg.instanceDocumentUrl == cfg.url

        when: "v2 is enabled"
        cfg.v2Enabled = true

        then: "v2 URs are set"
        cfg.url == OracleCloudMetadataConfiguration.DEFAULT_V2_URL
        cfg.vnicUrl == OracleCloudMetadataConfiguration.DEFAULT_V2_VNIC_URL
    }

    void 'it can determine the base url'(String url, String expectedBaseUrl) {
        given:
        def cfg = new OracleCloudMetadataConfiguration()
        cfg.url = url

        when:
        def baseUrl = cfg.baseUrl

        then:
        baseUrl == expectedBaseUrl

        where:
        url                                         || expectedBaseUrl
        "http://169.254.169.254/opc/v1/instance/"   || "http://169.254.169.254/opc/v1/"
        "http://169.254.169.254/opc/v1/instance"    || "http://169.254.169.254/opc/v1/"
        "http://169.254.169.254/opc/v2/instance/"   || "http://169.254.169.254/opc/v2/"
        "http://169.254.169.254/opc/v2/instance"    || "http://169.254.169.254/opc/v2/"
    }

}
