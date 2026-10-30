package io.micronaut.oraclecloud.notifications

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.Specification

@spock.lang.Requires({ System.getenv('NOTIFICATIONS_TOPIC_OCID') })
class OracleCloudNotificationServiceLiveSpec extends Specification {

    void "publishes notification to configured topic"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                'oci.notifications.topic-id': System.getenv('NOTIFICATIONS_TOPIC_OCID')
        ], Environment.ORACLE_CLOUD)
        def service = context.getBean(OracleCloudNotificationService)

        when:
        def response = service.publish('Micronaut Oracle Cloud test', 'Test message from micronaut-oracle-cloud.')

        then:
        response.opcRequestId

        cleanup:
        context.close()
    }
}
