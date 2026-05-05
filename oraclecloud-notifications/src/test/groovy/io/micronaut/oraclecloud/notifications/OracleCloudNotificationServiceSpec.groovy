package io.micronaut.oraclecloud.notifications

import com.oracle.bmc.ons.NotificationControlPlane
import com.oracle.bmc.ons.NotificationDataPlane
import com.oracle.bmc.ons.model.NotificationTopic
import com.oracle.bmc.ons.requests.GetTopicRequest
import com.oracle.bmc.ons.requests.PublishMessageRequest
import com.oracle.bmc.ons.responses.GetTopicResponse
import com.oracle.bmc.ons.responses.PublishMessageResponse
import io.micronaut.context.exceptions.ConfigurationException
import spock.lang.Specification

class OracleCloudNotificationServiceSpec extends Specification {

    void "publishes message to configured topic"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def configuration = new OracleCloudNotificationsConfiguration()
        configuration.topicId = 'ocid1.onstopic.oc1.phx.test'
        def service = new OracleCloudNotificationService(controlPlane, dataPlane, configuration)
        PublishMessageRequest request

        when:
        def response = service.publish('Test title', 'Test body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> { GetTopicRequest getTopicRequest ->
            assert getTopicRequest.topicId == 'ocid1.onstopic.oc1.phx.test'
            topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        }
        1 * dataPlane.getEndpoint() >> 'https://notification.us-phoenix-1.oci.oraclecloud.com'
        1 * dataPlane.setEndpoint('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        1 * dataPlane.publishMessage(_ as PublishMessageRequest) >> { PublishMessageRequest publishMessageRequest ->
            request = publishMessageRequest
            PublishMessageResponse.builder().opcRequestId('request-id').build()
        }
        1 * dataPlane.setEndpoint('https://notification.us-phoenix-1.oci.oraclecloud.com')
        response.opcRequestId == 'request-id'
        request.topicId == 'ocid1.onstopic.oc1.phx.test'
        request.messageDetails.title == 'Test title'
        request.messageDetails.body == 'Test body'
    }

    void "publishes message to explicit topic"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, dataPlane, new OracleCloudNotificationsConfiguration())
        PublishMessageRequest request

        when:
        service.publish('ocid1.onstopic.oc1.iad.test', 'Explicit title', 'Explicit body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> { GetTopicRequest getTopicRequest ->
            assert getTopicRequest.topicId == 'ocid1.onstopic.oc1.iad.test'
            topicResponse('https://cell1.notification.us-ashburn-1.oci.oraclecloud.com')
        }
        1 * dataPlane.getEndpoint() >> 'https://notification.us-ashburn-1.oci.oraclecloud.com'
        1 * dataPlane.setEndpoint('https://cell1.notification.us-ashburn-1.oci.oraclecloud.com')
        1 * dataPlane.publishMessage(_ as PublishMessageRequest) >> { PublishMessageRequest publishMessageRequest ->
            request = publishMessageRequest
            PublishMessageResponse.builder().opcRequestId('request-id').build()
        }
        1 * dataPlane.setEndpoint('https://notification.us-ashburn-1.oci.oraclecloud.com')
        request.topicId == 'ocid1.onstopic.oc1.iad.test'
        request.messageDetails.title == 'Explicit title'
        request.messageDetails.body == 'Explicit body'
    }

    void "publishes prebuilt request"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, dataPlane, new OracleCloudNotificationsConfiguration())
        def request = PublishMessageRequest.builder()
                .topicId('ocid1.onstopic.oc1.phx.test')
                .build()

        when:
        service.publish(request)

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        1 * dataPlane.getEndpoint() >> 'https://notification.us-phoenix-1.oci.oraclecloud.com'
        1 * dataPlane.setEndpoint('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        1 * dataPlane.publishMessage(request) >> PublishMessageResponse.builder().opcRequestId('request-id').build()
        1 * dataPlane.setEndpoint('https://notification.us-phoenix-1.oci.oraclecloud.com')
    }

    void "caches topic endpoint"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, dataPlane, new OracleCloudNotificationsConfiguration())

        when:
        service.publish('ocid1.onstopic.oc1.phx.test', 'First title', 'First body')
        service.publish('ocid1.onstopic.oc1.phx.test', 'Second title', 'Second body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        2 * dataPlane.getEndpoint() >> 'https://notification.us-phoenix-1.oci.oraclecloud.com'
        2 * dataPlane.setEndpoint('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        2 * dataPlane.publishMessage(_ as PublishMessageRequest) >> PublishMessageResponse.builder().opcRequestId('request-id').build()
        2 * dataPlane.setEndpoint('https://notification.us-phoenix-1.oci.oraclecloud.com')
    }

    void "requires topic endpoint"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, dataPlane, new OracleCloudNotificationsConfiguration())

        when:
        service.publish('ocid1.onstopic.oc1.phx.test', 'Test title', 'Test body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse(null)
        thrown ConfigurationException
    }

    void "requires configured topic for convenience publish method"() {
        given:
        def service = new OracleCloudNotificationService(Mock(NotificationControlPlane), Mock(NotificationDataPlane), new OracleCloudNotificationsConfiguration())

        when:
        service.publish('Test title', 'Test body')

        then:
        thrown ConfigurationException
    }

    private static GetTopicResponse topicResponse(String apiEndpoint) {
        GetTopicResponse.builder()
                .notificationTopic(NotificationTopic.builder()
                        .apiEndpoint(apiEndpoint)
                        .build())
                .build()
    }
}
