package io.micronaut.oraclecloud.logging

import com.oracle.bmc.Region
import com.oracle.bmc.loggingingestion.Logging
import com.oracle.bmc.loggingingestion.LoggingClient
import com.oracle.bmc.loggingingestion.model.LogEntry
import com.oracle.bmc.loggingingestion.model.LogEntryBatch
import com.oracle.bmc.loggingingestion.requests.PutLogsRequest
import com.oracle.bmc.loggingingestion.responses.PutLogsResponse
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.discovery.ServiceInstance
import io.micronaut.discovery.event.ServiceReadyEvent
import io.micronaut.runtime.ApplicationConfiguration
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@MicronautTest
class OracleCloudLoggingSpec extends Specification {

    @Inject
    Logging logging

    @Inject
    ApplicationEventPublisher<ServiceReadyEvent> eventPublisher

    @Inject
    ApplicationConfiguration applicationConfiguration

    void "test oracle cloud logging"() {
        given:
        def logMessage = 'test logging'
        def testHost = 'testHost'
        def logger = LoggerFactory.getLogger(OracleCloudLoggingSpec.class)
        PollingConditions conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

        def instance = Mock(ServiceInstance.class)
        def event = new ServiceReadyEvent(instance)
        def mockLogging = (MockLogging) logging
        1 * instance.getHost() >> testHost
        eventPublisher.publishEvent(event)

        when:
        logger.info(logMessage)

        then:
        logging.endpoint == 'mock-logging-endpoint'
        conditions.eventually {
            mockLogging.getPutLogsRequestList().size() != 0
        }

        def list = ((MockLogging) logging).getPutLogsRequestList()
        list.stream().allMatch(x -> x.logId == 'test-log-id')
        def logEntries = new ArrayList<LogEntry>()
        def logEntryBatch = new ArrayList<LogEntryBatch>()
        list.putLogsDetails.logEntryBatches.forEach(
                x -> {
                    logEntryBatch.addAll(x)
                    x.stream().forEach(y -> logEntries.addAll(y.entries))
                }
        )
        logEntryBatch.stream().allMatch(x -> x.source == testHost)
        logEntryBatch.stream().allMatch(x -> x.type == applicationConfiguration.getName().get() + '.' + testHost + '.INFO')
        logEntryBatch.stream().anyMatch(x -> x.subject == 'io.micronaut.context.env.DefaultEnvironment')
        logEntryBatch.stream().anyMatch(x -> x.subject == 'io.micronaut.context.DefaultBeanContext')
        logEntryBatch.stream().anyMatch(x -> x.subject == 'io.micronaut.oraclecloud.logging.OracleCloudLoggingSpec')

        logEntries.stream().anyMatch(x -> x.data.contains('io.micronaut.context.env.DefaultEnvironment'))
        logEntries.stream().anyMatch(x -> x.data.contains('io.micronaut.context.DefaultBeanContext'))
        logEntries.stream().anyMatch(x -> x.data.contains('io.micronaut.oraclecloud.logging.OracleCloudLoggingSpec'))
        logEntries.stream().anyMatch(x -> x.data.contains(logMessage))
        logEntries.stream().anyMatch(x -> x.data.contains('Established active environments'))
        logEntries.stream().anyMatch(x -> x.data.contains('Reading bootstrap environment configuration'))
        MockAppender.getEvents().size() == 0

        when:
        mockLogging.setSuccess(false)
        logger.info(logMessage)
        conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

        then:
        conditions.eventually {
            MockAppender.getEvents().size() != 0
        }
        MockAppender.getEvents().get(0).message == logMessage
    }


    @Singleton
    @Replaces(LoggingClient)
    static class MockLogging implements Logging {

        final List<PutLogsRequest> putLogsRequestList = Collections.synchronizedList(new ArrayList<>())

        private boolean success = true

        @Override
        void refreshClient() {

        }

        @Override
        void setEndpoint(String endpoint) {

        }

        boolean getSuccess() {
            return success
        }

        void setSuccess(boolean success) {
            this.success = success
        }

        @Override
        String getEndpoint() {
            return 'mock-logging-endpoint'
        }

        @Override
        void setRegion(Region region) {

        }

        @Override
        void setRegion(String regionId) {

        }

        List<PutLogsRequest> getPutLogsRequestList() {
            synchronized (putLogsRequestList) {
                return new ArrayList<PutLogsRequest>(putLogsRequestList)
            }
        }

        @Override
        PutLogsResponse putLogs(PutLogsRequest request) {
            synchronized (putLogsRequestList) {
                putLogsRequestList.add(request)
            }
            if (success) {
                return PutLogsResponse.builder().opcRequestId("validId").build()
            }
            return PutLogsResponse.builder().opcRequestId(null).__httpStatusCode__(404).build()
        }

        @Override
        void close() throws Exception {

        }
    }

}
