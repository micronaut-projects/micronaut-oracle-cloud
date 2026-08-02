package io.micronaut.oraclecloud.queue

import com.oracle.bmc.queue.model.GetMessage
import com.oracle.bmc.queue.model.PutMessage
import groovy.transform.CompileStatic
import io.micronaut.oraclecloud.queue.annotation.Queue
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

@MicronautTest
class QueueSpec extends Specification {

    @Shared
    @Inject
    TestQueue1 queue

    @Shared
    @Inject
    TestQueue1Abstract queueAbstract

    void 'post message to queue and retrieve'() {
        given:
        String messageContent = 'Brilliant!'
        queue.putMessage messageContent

        when:
        GetMessage message = queue.waitMessage()

        queue.updateMessage message.receipt, 60

        then:
        message
        message.content == messageContent

        cleanup:
        queue.deleteMessage message.receipt
    }

    void 'alternative post message to queue and retrieve'() {
        given:
        String testMessageContent = 'Fantastic!'
        queueAbstract.putWithChecksum null, testMessageContent, null

        when:
        GetMessage message = queueAbstract.waitMessage()

        queueAbstract.updateMessage message, 60

        then:
        message
        message.content == testMessageContent
        message.metadata.customProperties.containsKey('checksum')
        TestUtils.sha256(testMessageContent) == message.metadata.customProperties.checksum

        cleanup:
        queueAbstract.deleteMessage message
    }
}

@Queue(name = 'dev-testQueue1')
@CompileStatic
interface TestQueue1 extends GenericQueue {
}

@Queue(name = 'dev-testQueue1')
@CompileStatic
abstract class TestQueue1Abstract implements GenericQueue {

    PutMessage putWithChecksum(String channel, String message, Map<String, String> metadata) {
        if (metadata == null) {
            metadata = [:]
        }
        metadata.checksum = TestUtils.sha256(message)
        return putMessage(channel, message, metadata)
    }
}
