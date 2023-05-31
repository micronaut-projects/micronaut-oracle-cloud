package io.micronaut.oraclecloud.certificates

import io.micronaut.oraclecloud.certificates.background.OracleCloudCertificationRefresherTask
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateService
import spock.lang.Specification

class OracleCloudCertificateRefresherTaskSpec extends Specification {

    def "task refreshCertificate calls refreshCertificate on the service"() {
        given:
        def mockOracleCloudService = Mock(OracleCloudCertificateService)
        def task = new OracleCloudCertificationRefresherTask(mockOracleCloudService)

        when:
        task.refreshCertificate()

        then:
        1 * mockOracleCloudService.refreshCertificate()

    }
}
