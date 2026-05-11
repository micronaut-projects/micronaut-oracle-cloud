package io.micronaut.oraclecloud.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.read.ListAppender
import com.oracle.bmc.loggingingestion.Logging
import com.oracle.bmc.loggingingestion.model.LogEntry
import com.oracle.bmc.loggingingestion.model.LogEntryBatch
import io.micronaut.context.annotation.Property
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.runtime.ApplicationConfiguration
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@MicronautTest
@Property(name = "spec.name", value = "OracleCloudLoggingSpec")
@Property(name = "oci.logging.logId", value = "test-logId-from-application-config")
@Property(name = "app.filepath", value = "classpath:data/addresses.csv")
class OracleCloudLoggingSpec extends Specification {

    @Inject
    Logging logging

    @Inject
    ApplicationEventPublisher<ServerStartupEvent> eventPublisher

    @Inject
    ApplicationConfiguration applicationConfiguration

    @Inject
    ReadableConfiguration readableConfiguration

    void "test oracle cloud logging"() {
        given:
        def logMessage = 'test logging'
        def testHost = 'testHost'
        def logger = LoggerFactory.getLogger(OracleCloudLoggingSpec.class)
        def contextLogger = LoggerFactory.getLogger('io.micronaut.context.env.DefaultEnvironment')
        PollingConditions conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory()
        ListAppender listAppender
        OracleCloudAppender oracleCloudAppender
        loggerContext.loggerList.each { Logger l ->
            l.iteratorForAppenders().each { appender ->
                if (appender.name == 'ORACLE') {
                    oracleCloudAppender = (OracleCloudAppender) appender
                    listAppender = (ListAppender) oracleCloudAppender.getAppender('MOCK')
                }
            }
        }
        oracleCloudAppender.@configuredSuccessfully = false
        oracleCloudAppender.source = null
        oracleCloudAppender.type = null
        oracleCloudAppender.subject = null
        listAppender.list.clear()

        when:
        def instance = Mock(EmbeddedServer.class)
        def event = new ServerStartupEvent(instance)
        def mockLogging = (MockLogging) logging
        1 * instance.getHost() >> testHost
        eventPublisher.publishEvent(event)
        contextLogger.info('Established active environments: [oraclecloud]')
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
        String expectedType = "${testHost}.${applicationConfiguration.getName().get()}"
        logEntryBatch.stream().allMatch(x -> expectedType.equals(x.type))
        logEntryBatch.stream().anyMatch(x -> x.subject == applicationConfiguration.getName().get())

        logEntries.stream().anyMatch(x -> x.data.contains('io.micronaut.context'))
        logEntries.stream().anyMatch(x -> x.data.contains('io.micronaut.oraclecloud.logging.OracleCloudLoggingSpec'))
        logEntries.stream().anyMatch(x -> x.data.contains(logMessage))
        logEntries.stream().anyMatch(x -> x.data.contains('Established active environments'))
        readableConfiguration.readable.exists()
        readableConfiguration.readable.name.endsWith("addresses.csv")
        listAppender.list.size() == 0

        when:
        mockLogging.setSuccess(false)
        logger.info(logMessage)
        conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

        then:
        conditions.eventually {
            listAppender.list.size() != 0
        }
        listAppender.list.get(0).message == logMessage
    }

}
