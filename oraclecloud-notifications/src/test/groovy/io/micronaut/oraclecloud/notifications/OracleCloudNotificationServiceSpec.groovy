package io.micronaut.oraclecloud.notifications

import com.oracle.bmc.ons.NotificationControlPlane
import com.oracle.bmc.ons.NotificationDataPlane
import com.oracle.bmc.ons.model.MessageDetails
import com.oracle.bmc.ons.model.NotificationTopic
import com.oracle.bmc.ons.requests.GetTopicRequest
import com.oracle.bmc.ons.requests.PublishMessageRequest
import com.oracle.bmc.ons.responses.GetTopicResponse
import com.oracle.bmc.ons.responses.PublishMessageResponse
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.exceptions.ConfigurationException
import spock.lang.Specification

class OracleCloudNotificationServiceSpec extends Specification {

    void "publishes message to configured topic"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def configuration = new OracleCloudNotificationsConfiguration()
        configuration.topicId = 'ocid1.onstopic.oc1.phx.test'
        def endpointFactory = dataPlaneFactory(dataPlane)
        def service = new OracleCloudNotificationService(controlPlane, endpointFactory, configuration)
        PublishMessageRequest request

        when:
        def response = service.publish('Test title', 'Test body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> { GetTopicRequest getTopicRequest ->
            assert getTopicRequest.topicId == 'ocid1.onstopic.oc1.phx.test'
            topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        }
        endpointFactory.endpoints == ['https://cell1.notification.us-phoenix-1.oci.oraclecloud.com']
        1 * dataPlane.publishMessage(_ as PublishMessageRequest) >> { PublishMessageRequest publishMessageRequest ->
            request = publishMessageRequest
            PublishMessageResponse.builder().opcRequestId('request-id').build()
        }
        0 * dataPlane.setEndpoint(_)
        0 * dataPlane.getEndpoint()
        response.opcRequestId == 'request-id'
        request.topicId == 'ocid1.onstopic.oc1.phx.test'
        request.messageDetails.title == 'Test title'
        request.messageDetails.body == 'Test body'
    }

    void "wires service from application context with replaceable endpoint factory"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def endpointFactory = dataPlaneFactory(dataPlane)
        NotificationsTestFactory.controlPlane = controlPlane
        NotificationsTestFactory.dataPlaneFactoryBean = endpointFactory
        ApplicationContext context = ApplicationContext.run([
                'spec.name': 'OracleCloudNotificationServiceSpec',
                'oci.notifications.topic-id': 'ocid1.onstopic.oc1.phx.test'
        ])
        def service = context.getBean(OracleCloudNotificationService)
        PublishMessageRequest request

        when:
        service.publish('Test title', 'Test body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        endpointFactory.endpoints == ['https://cell1.notification.us-phoenix-1.oci.oraclecloud.com']
        1 * dataPlane.publishMessage(_ as PublishMessageRequest) >> { PublishMessageRequest publishMessageRequest ->
            request = publishMessageRequest
            PublishMessageResponse.builder().opcRequestId('request-id').build()
        }
        request.topicId == 'ocid1.onstopic.oc1.phx.test'

        cleanup:
        context?.close()
        NotificationsTestFactory.controlPlane = null
        NotificationsTestFactory.dataPlaneFactoryBean = null
    }

    void "publishes message to explicit topic"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def endpointFactory = dataPlaneFactory(dataPlane)
        def service = new OracleCloudNotificationService(controlPlane, endpointFactory, new OracleCloudNotificationsConfiguration())
        PublishMessageRequest request

        when:
        service.publish('ocid1.onstopic.oc1.iad.test', 'Explicit title', 'Explicit body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> { GetTopicRequest getTopicRequest ->
            assert getTopicRequest.topicId == 'ocid1.onstopic.oc1.iad.test'
            topicResponse('https://cell1.notification.us-ashburn-1.oci.oraclecloud.com')
        }
        endpointFactory.endpoints == ['https://cell1.notification.us-ashburn-1.oci.oraclecloud.com']
        1 * dataPlane.publishMessage(_ as PublishMessageRequest) >> { PublishMessageRequest publishMessageRequest ->
            request = publishMessageRequest
            PublishMessageResponse.builder().opcRequestId('request-id').build()
        }
        0 * dataPlane.setEndpoint(_)
        0 * dataPlane.getEndpoint()
        request.topicId == 'ocid1.onstopic.oc1.iad.test'
        request.messageDetails.title == 'Explicit title'
        request.messageDetails.body == 'Explicit body'
    }

    void "publishes prebuilt request"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, dataPlaneFactory(dataPlane), new OracleCloudNotificationsConfiguration())
        def request = PublishMessageRequest.builder()
                .topicId('ocid1.onstopic.oc1.phx.test')
                .messageDetails(MessageDetails.builder()
                        .title('Test title')
                        .body('Test body')
                        .build())
                .build()

        when:
        service.publish(request)

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        1 * dataPlane.publishMessage(request) >> PublishMessageResponse.builder().opcRequestId('request-id').build()
        0 * dataPlane.setEndpoint(_)
        0 * dataPlane.getEndpoint()
    }

    void "caches topic endpoint and endpoint client"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def endpointFactory = dataPlaneFactory(dataPlane)
        def service = new OracleCloudNotificationService(controlPlane, endpointFactory, new OracleCloudNotificationsConfiguration())

        when:
        service.publish('ocid1.onstopic.oc1.phx.test', 'First title', 'First body')
        service.publish('ocid1.onstopic.oc1.phx.test', 'Second title', 'Second body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        endpointFactory.endpoints == ['https://cell1.notification.us-phoenix-1.oci.oraclecloud.com']
        2 * dataPlane.publishMessage(_ as PublishMessageRequest) >> PublishMessageResponse.builder().opcRequestId('request-id').build()
        0 * dataPlane.setEndpoint(_)
        0 * dataPlane.getEndpoint()
    }

    void "requires topic endpoint"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, dataPlaneFactory(dataPlane), new OracleCloudNotificationsConfiguration())

        when:
        service.publish('ocid1.onstopic.oc1.phx.test', 'Test title', 'Test body')

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse(null)
        thrown ConfigurationException
    }

    void "requires configured topic for convenience publish method"() {
        given:
        def service = new OracleCloudNotificationService(Mock(NotificationControlPlane), dataPlaneFactory(Mock(NotificationDataPlane)), new OracleCloudNotificationsConfiguration())

        when:
        service.publish('Test title', 'Test body')

        then:
        thrown ConfigurationException
    }

    void "requires non-empty publish arguments"(String topicId, String title, String body) {
        given:
        def service = new OracleCloudNotificationService(Mock(NotificationControlPlane), dataPlaneFactory(Mock(NotificationDataPlane)), new OracleCloudNotificationsConfiguration())

        when:
        service.publish(topicId, title, body)

        then:
        thrown IllegalArgumentException

        where:
        topicId                       | title        | body
        ''                            | 'Test title' | 'Test body'
        'ocid1.onstopic.oc1.phx.test' | ''           | 'Test body'
        'ocid1.onstopic.oc1.phx.test' | 'Test title' | ''
    }

    void "closes cached endpoint clients"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def dataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, dataPlaneFactory(dataPlane), new OracleCloudNotificationsConfiguration())

        when:
        service.publish('ocid1.onstopic.oc1.phx.test', 'Test title', 'Test body')
        service.close()

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        1 * dataPlane.publishMessage(_ as PublishMessageRequest) >> PublishMessageResponse.builder().opcRequestId('request-id').build()
        1 * dataPlane.close()
    }

    void "closes every cached endpoint client and reports close failures"() {
        given:
        def controlPlane = Mock(NotificationControlPlane)
        def firstDataPlane = Mock(NotificationDataPlane)
        def secondDataPlane = Mock(NotificationDataPlane)
        def service = new OracleCloudNotificationService(controlPlane, new RecordingDataPlaneFactory([
                'https://cell1.notification.us-phoenix-1.oci.oraclecloud.com': firstDataPlane,
                'https://cell2.notification.us-ashburn-1.oci.oraclecloud.com': secondDataPlane
        ]), new OracleCloudNotificationsConfiguration())

        when:
        service.publish('ocid1.onstopic.oc1.phx.test', 'First title', 'First body')
        service.publish('ocid1.onstopic.oc1.iad.test', 'Second title', 'Second body')
        service.close()

        then:
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell1.notification.us-phoenix-1.oci.oraclecloud.com')
        1 * controlPlane.getTopic(_ as GetTopicRequest) >> topicResponse('https://cell2.notification.us-ashburn-1.oci.oraclecloud.com')
        1 * firstDataPlane.publishMessage(_ as PublishMessageRequest) >> PublishMessageResponse.builder().opcRequestId('request-id-1').build()
        1 * secondDataPlane.publishMessage(_ as PublishMessageRequest) >> PublishMessageResponse.builder().opcRequestId('request-id-2').build()
        1 * firstDataPlane.close() >> { throw new IOException('first close failed') }
        1 * secondDataPlane.close() >> { throw new IOException('second close failed') }
        def exception = thrown(IOException)
        ([exception.message] + exception.suppressed*.message) as Set == ['first close failed', 'second close failed'] as Set
    }

    private static GetTopicResponse topicResponse(String apiEndpoint) {
        GetTopicResponse.builder()
                .notificationTopic(NotificationTopic.builder()
                        .apiEndpoint(apiEndpoint)
                        .build())
                .build()
    }

    private static RecordingDataPlaneFactory dataPlaneFactory(NotificationDataPlane dataPlane) {
        new RecordingDataPlaneFactory(dataPlane)
    }

    @Factory
    @Requires(property = 'spec.name', value = 'OracleCloudNotificationServiceSpec')
    static class NotificationsTestFactory {
        static NotificationControlPlane controlPlane
        static NotificationDataPlaneFactory dataPlaneFactoryBean

        @Bean
        @Replaces(NotificationControlPlane)
        NotificationControlPlane notificationControlPlane() {
            controlPlane
        }

        @Bean
        @Replaces(DefaultNotificationDataPlaneFactory)
        NotificationDataPlaneFactory notificationDataPlaneFactory() {
            dataPlaneFactoryBean
        }
    }

    private static final class RecordingDataPlaneFactory implements NotificationDataPlaneFactory {
        private final NotificationDataPlane defaultDataPlane
        private final Map<String, NotificationDataPlane> dataPlanes
        private final List<String> endpoints = []

        private RecordingDataPlaneFactory(NotificationDataPlane dataPlane) {
            this.defaultDataPlane = dataPlane
            this.dataPlanes = [:]
        }

        private RecordingDataPlaneFactory(Map<String, NotificationDataPlane> dataPlanes) {
            this.defaultDataPlane = null
            this.dataPlanes = dataPlanes
        }

        @Override
        NotificationDataPlane create(String endpoint) {
            endpoints << endpoint
            dataPlanes.getOrDefault(endpoint, defaultDataPlane)
        }

        List<String> getEndpoints() {
            endpoints
        }
    }

}
