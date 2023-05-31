package io.micronaut.oraclecloud.certificates

import io.micronaut.oraclecloud.certificates.background.OracleCloudCertRefresherTask
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateService
import spock.lang.Specification

class OracleCloudRefresherTaskSpec extends Specification {

    def "task refreshCertificate calls refreshCertificate on the service"() {
        given:
        def mockOracleCloudService = Mock(OracleCloudCertificateService)
        def task = new OracleCloudCertRefresherTask(mockOracleCloudService)

        when:
        task.refreshCertificate()

        then:
        1 * mockOracleCloudService.refreshCertificate()

    }
}
