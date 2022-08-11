package io.micronaut.oraclecloud.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import spock.lang.Specification


class OracleCloudLoggingAppenderSpec extends Specification {

    def appender = new OracleCloudAppender()
    def context = new LoggerContext()


    def setup() {
        appender = new OracleCloudAppender()
        context = new LoggerContext()
        appender.setContext(context)
    }

    def cleanup() {
        appender.stop()
    }

    void 'test error queue size less then 0'() {
        when:
        appender.setQueueSize(-1)
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "Queue size must be greater than zero" }
    }

    void 'test error queue size equal to 0'() {
        when:
        appender.setQueueSize(0)
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "Queue size of zero is deprecated, use a size of one to indicate synchronous processing" }
    }

    void 'test error publish period less or equal to 0'() {
        when:
        appender.setQueueSize(100)
        appender.setPublishPeriod(0)
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "Publish period must be greater than zero" }
    }

    void 'encoder not set'() {
        when:
        appender.setQueueSize(100)
        appender.setPublishPeriod(100)
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "No encoder set for the appender named [null]." }
    }

    void 'log id not set'() {
        when:
        appender.setQueueSize(100)
        appender.setPublishPeriod(100)
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.start()

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
        statuses.find { it.message == "LogId not specified" }
    }

    void 'register multiple emergency appender'() {
        when:
        def logId = "testLogId"
        def mockAppender = new MockAppender()
        appender.setQueueSize(100)
        appender.setPublishPeriod(101)
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.setLogId(logId)
        appender.addAppender(mockAppender)
        appender.addAppender(mockAppender)

        then:
        def statuses = context.getStatusManager().getCopyOfStatusList()
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
        appender.setQueueSize(100)
        appender.setPublishPeriod(100)
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.setLogId("testLogId")
        appender.addAppender(mockAppender)

        then:
        appender.detachAppender("MockAppender")
        !appender.detachAppender("NotExistingOne")
    }

    void 'detach emergency appender by instance'() {
        when:
        def mockAppender = new MockAppender()
        appender.setQueueSize(100)
        appender.setPublishPeriod(100)
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.setLogId("testLogId")
        appender.addAppender(mockAppender)

        then:
        appender.detachAppender(mockAppender)
        !appender.detachAppender(mockAppender)
    }

    void 'try to create iterator for emergency appender'() {
        when:
        def mockAppender = new MockAppender()
        appender.setQueueSize(100)
        appender.setPublishPeriod(100)
        appender.setEncoder(new LayoutWrappingEncoder())
        appender.setLogId("testLogId")
        appender.addAppender(mockAppender)
        appender.iteratorForAppenders()

        then:
        thrown(UnsupportedOperationException)
    }

}
