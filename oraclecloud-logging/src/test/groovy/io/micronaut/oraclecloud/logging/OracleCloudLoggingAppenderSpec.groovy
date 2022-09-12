package io.micronaut.oraclecloud.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import io.micronaut.discovery.ServiceInstance
import io.micronaut.discovery.event.ServiceReadyEvent
import io.micronaut.runtime.ApplicationConfiguration
import spock.lang.Specification
import spock.util.concurrent.PollingConditions


class OracleCloudLoggingAppenderSpec extends Specification {

    OracleCloudAppender appender
    LoggerContext context
    PatternLayout layout
    LayoutWrappingEncoder encoder
    OracleCloudLoggingSpec.MockLogging oracleCloudLogsClient

    def setup() {
        context = new LoggerContext()
        layout = new PatternLayout()
        layout.context = context
        layout.pattern = "[%thread] %level %logger{20} - %msg%n%xThrowable"
        layout.start()
        encoder = new LayoutWrappingEncoder()
        encoder.layout = layout
        encoder.start()
        appender = new OracleCloudAppender()
        appender.context = context
        appender.encoder = encoder
        def config = Stub(ApplicationConfiguration) {
            getName() >> Optional.of("my-awesome-app")
        }
        def instance = Mock(ServiceInstance.class)
        instance.getHost() >> "testHost"
        def serviceReadyEvent = new ServiceReadyEvent(instance)

        oracleCloudLogsClient = new OracleCloudLoggingSpec.MockLogging()

        new OracleCloudLoggingClient(oracleCloudLogsClient, config, Optional.empty()).onApplicationEvent(serviceReadyEvent)

    }

    def cleanup() {
        layout.stop()
        encoder.stop()
        appender.stop()
        OracleCloudLoggingClient.destroy()
    }

    void 'test error queue size less then 0'() {
        when:
        appender.queueSize = -1
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "Queue size must be greater than zero" }
    }

    void 'test error queue size equal to 0'() {
        when:
        appender.queueSize = 0
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "Queue size of zero is deprecated, use a size of one to indicate synchronous processing" }
    }

    void 'test error max batch size less or equal to 0'() {
        when:
        appender.maxBatchSize = 0
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "Max Batch size must be greater than zero" }
    }

    void 'test error publish period less or equal to 0'() {
        when:
        appender.queueSize = 100
        appender.publishPeriod = 0
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "Publish period must be greater than zero" }
    }

    void 'encoder not set'() {
        when:
        appender.queueSize = 100
        appender.publishPeriod = 100
        appender.encoder = null
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "No encoder set for the appender named [null]." }
    }

    void 'log id not set'() {
        when:
        appender.queueSize = 100
        appender.publishPeriod = 100
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "LogId is not specified in logback configuration it might be fetch from application configuration if available" }
    }

    void 'register multiple emergency appender'() {
        when:
        def logId = "testLogId"
        def mockAppender = new MockAppender()
        appender.queueSize = 100
        appender.publishPeriod = 101
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.logId = logId
        appender.addAppender(mockAppender)
        appender.addAppender(mockAppender)

        then:
        def statuses = context.statusManager.getCopyOfStatusList()
        statuses.find { it.message == "One and only one appender may be attached to OracleCloudAppender" }
        statuses.find { it.message == "Ignoring additional appender named [MockAppender]" }
        appender.getAppender("MockAppender") != null
        appender.getAppender("NotExistingOne") == null
        appender.isAttached(mockAppender)
        appender.getEncoder() != null
        appender.getLogId() == logId
        appender.getQueueSize() == 100
        appender.getPublishPeriod() == 101

        appender.detachAndStopAllAppenders()
        !appender.isAttached(mockAppender)
    }

    void 'detach emergency appender by name'() {
        when:
        def mockAppender = new MockAppender()
        appender.queueSize = 100
        appender.publishPeriod = 100
        appender.encoder = new LayoutWrappingEncoder()
        appender.logId = "testLogId"
        appender.addAppender(mockAppender)

        then:
        appender.detachAppender("MockAppender")
        !appender.detachAppender("NotExistingOne")
    }

    void 'detach emergency appender by instance'() {
        when:
        def mockAppender = new MockAppender()
        appender.queueSize = 100
        appender.publishPeriod = 100
        appender.encoder = new LayoutWrappingEncoder()
        appender.logId = "testLogId"
        appender.addAppender(mockAppender)

        then:
        appender.detachAppender(mockAppender)
        !appender.detachAppender(mockAppender)
    }

    void 'try to create iterator for emergency appender'() {
        when:
        def mockAppender = new MockAppender()
        appender.queueSize = 100
        appender.publishPeriod = 100
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.logId = "testLogId"
        appender.addAppender(mockAppender)
        appender.iteratorForAppenders()

        then:
        thrown(UnsupportedOperationException)
    }

    void 'custom subject, type and and source of log'() {
        given:
        def testSubject = "testSubject"
        def testType = "testType"
        def testSource = "testSource"
        def testMessage = "testMessage"
        appender.logId = "testLogId"
        PollingConditions conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)
        LoggingEvent event = createEvent("name", Level.INFO, testMessage, System.currentTimeMillis())

        when:
        appender.subject = testSubject
        appender.type = testType
        appender.source = testSource
        appender.start()
        appender.doAppend(event)

        then:
        appender.subject == testSubject
        appender.type == testType
        appender.source == testSource
        conditions.eventually {
            oracleCloudLogsClient.putLogsRequestList.size() == 1
        }
        oracleCloudLogsClient.putLogsRequestList.get(0).putLogsDetails.logEntryBatches.get(0).subject == testSubject
        oracleCloudLogsClient.putLogsRequestList.get(0).putLogsDetails.logEntryBatches.get(0).type == testType
        oracleCloudLogsClient.putLogsRequestList.get(0).putLogsDetails.logEntryBatches.get(0).source == testSource

    }

    LoggingEvent createEvent(String name, Level level, String message, Long time) {
        LoggingEvent event = new LoggingEvent()
        event.loggerName = name
        event.level = level
        event.message = message
        if (time != null) {
            event.timeStamp = time
        }
        return event
    }
}
