package io.micronaut.oraclecloud.queue

import com.oracle.bmc.queue.model.GetMessage
import com.oracle.bmc.queue.model.PutMessage
import groovy.transform.CompileStatic
import io.micronaut.oraclecloud.queue.annotation.Queue
import io.micronaut.oraclecloud.queue.annotation.QueueListener
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import spock.lang.Specification

@MicronautTest
class QueueListenerSpec extends Specification {

    @Shared
    @Inject
    TestQueue2Listener queue2Listener

    @Shared
    @Inject
    TestQueue3 queue3

    @Shared
    @Inject
    TestQueue3ListenerSales queue3ListenerSales

    @Shared
    @Inject
    TestQueue3ListenerMarketing queue3ListenerMarketing

    void 'post message to queue and retrieve'() {
        given:
        String messageContent1 = 'Fabulous!'
        PutMessage response1 = queue2Listener.putMessage(messageContent1)

        String message2Content = 'Splendid!'
        PutMessage response2 = queue2Listener.putMessage(message2Content)

        when:
        sleep 2000
        List<Long> messages = queue2Listener.getReceivedMessages()
        GetMessage message1 = queue2Listener.getMessage()
        GetMessage message2 = queue2Listener.getMessage()

        then:
        messages
        messages.contains response1.id
        messages.contains response2.id
        messages.size() == 2
        !message1
        !message2
    }

    void 'post message to queue associated with channel-specific listener and retrieve'() {
        given:
        String messageContent1 = 'Magnificent!'
        PutMessage response1 = queue3.putMessage('Sales', messageContent1)

        String message2Content = 'Spectacular!'
        PutMessage response2 = queue3.putMessage('Marketing', message2Content)

        when:
        sleep 2000
        List<Long> salesMessages = queue3ListenerSales.getReceivedMessages()
        List<Long> marketingMessages = queue3ListenerMarketing.getReceivedMessages()
        GetMessage message1 = queue3.getMessage('Sales')
        GetMessage message2 = queue3.getMessage('Marketing')

        then:
        salesMessages
        salesMessages.size() == 1
        salesMessages.contains response1.id

        marketingMessages
        marketingMessages.size() == 1
        marketingMessages.contains response2.id
        !message1
        !message2
    }
}

abstract class BaseListener implements GenericQueueListener {

    final List<Long> receivedMessages = []

    @Override
    void onMessageReceived(GetMessage message) {
        receivedMessages << message.id
    }
}

@QueueListener(
        name = 'dev-testQueue2',
        proceedIfExpired = false,
        proceedIfVisible = false,
        autoExtendLease = true,
        autoExtendLeaseSeconds = 60,
        autoDelete = true)
@CompileStatic
abstract class TestQueue2Listener extends BaseListener {
}

@Queue(name = 'dev-testQueue3')
@CompileStatic
interface TestQueue3 extends GenericQueue {}

@QueueListener(
        name = 'dev-testQueue3',
        channel = 'Marketing',
        proceedIfExpired = false,
        proceedIfVisible = false,
        autoExtendLease = false,
        autoDelete = true)
@CompileStatic
abstract class TestQueue3ListenerMarketing extends BaseListener {
}

@QueueListener(
        name = 'dev-testQueue3',
        channel = 'Sales',
        proceedIfExpired = false,
        proceedIfVisible = false,
        autoExtendLease = false,
        autoDelete = true)
@CompileStatic
abstract class TestQueue3ListenerSales extends BaseListener {
}
