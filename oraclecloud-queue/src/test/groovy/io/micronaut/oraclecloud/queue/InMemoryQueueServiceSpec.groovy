package io.micronaut.oraclecloud.queue

import com.oracle.bmc.queue.model.GetMessage
import com.oracle.bmc.queue.model.Queue
import com.oracle.bmc.queue.model.QueueSummary
import com.oracle.bmc.queue.responses.GetMessagesResponse
import com.oracle.bmc.queue.responses.GetQueueResponse
import com.oracle.bmc.queue.responses.ListQueuesResponse
import groovy.util.logging.Slf4j
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration.QueueConfig
import io.micronaut.oraclecloud.queue.service.QueueService
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

@Slf4j('LOG')
@MicronautTest
class InMemoryQueueServiceSpec extends Specification {

    @Shared
    @Inject
    OracleQueueConfiguration config

    @Shared
    @Inject
    QueueService service

    void 'queue creation and lookup'() {
        given:
        String name = 'test-newQueue'
        int retentionSeconds = 3600
        int visibilitySeconds = 240
        int deadLetterQueueDeliveryCount = 2
        String ocid = service.createQueue(config.compartmentOcid, name,
                retentionSeconds, visibilitySeconds, deadLetterQueueDeliveryCount)

        when:
        Queue queue = service.getQueue(ocid).queue

        then:
        queue.id == ocid
        queue.displayName == name
        queue.retentionInSeconds == retentionSeconds
        queue.visibilityInSeconds == visibilitySeconds
        queue.deadLetterQueueDeliveryCount == deadLetterQueueDeliveryCount

        cleanup:
        service.deleteQueue ocid, false
    }

    void 'lookup queue from config'() {
        given:
        String name = 'dev-testQueue1'

        when:
        QueueConfig queueConfig = config.queues.find { it.name == name }

        then:
        queueConfig

        when:
        GetQueueResponse response = service.getQueue(queueConfig.ocid)

        then:
        response.queue.id == queueConfig.ocid
    }

    void 'post message to queue and retrieve'() {
        given:
        String queueOcid = 'ocid1.queue.oc1.in.memory.123'
        String messageContent = 'Genius!'
        String channel = null
        Map<String, String> metadata = null

        service.putMessage queueOcid, channel, messageContent, metadata

        when:
        GetMessagesResponse response = service.waitMessages(queueOcid, channel, 30, 20, 30)
        List<GetMessage> messages = response.getMessages.messages

        then:
        messages
        messages.find { it.content == messageContent }
    }

    void 'create queue and post message and retrieve'() {
        given:
        String queueName = 'dev-secondInMemoryQueue'
        int retentionSeconds = 3600
        int visibilitySeconds = 5
        int deadLetterQueueDeliveryCount = 2

        String queueOcid = service.createQueue(config.compartmentOcid, queueName,
                retentionSeconds, visibilitySeconds, deadLetterQueueDeliveryCount)

        String messageContent = 'Test123!'
        String channel = config.namespace + '/XXX'
        Map<String, String> metadata = null

        service.putMessage queueOcid, channel, messageContent, metadata

        when:
        GetMessagesResponse messageResponse = service.waitMessages(queueOcid, channel, 30, 20)
        List<GetMessage> messages = messageResponse.getMessages.messages

        messageResponse = service.waitMessages(queueOcid, channel, 30, 20)
        messages = messageResponse.getMessages.messages

        sleep 5000
        messageResponse = service.waitMessages(queueOcid, channel, 30, 20)
        messages = messageResponse.getMessages.messages

        messageResponse = service.waitMessages(queueOcid, channel, 30, 20)
        messages = messageResponse.getMessages.messages

        sleep 5000
        messageResponse = service.waitMessages(queueOcid, channel, 30, 20)
        messages = messageResponse.getMessages.messages

        then:
        messages
        messages[0].deliveryCount == 3
        GetMessage message = messages.stream()
                .filter({ it.content == messageContent })
                .findFirst()
                .orElseThrow()

        cleanup:
        service.deleteQueue queueOcid, false
    }

    void 'create queues and list all queues'() {
        given:
        int retentionSeconds = 3600
        int visibilitySeconds = 5
        int dlqDeliveryCount = 2
        String queue1Ocid = service.createQueue('dev-mem4', retentionSeconds, visibilitySeconds, dlqDeliveryCount)
        String queue2Ocid = service.createQueue('dev-mem2', retentionSeconds, visibilitySeconds, dlqDeliveryCount)
        String queue3Ocid = service.createQueue('dev-mem3', retentionSeconds, visibilitySeconds, dlqDeliveryCount)
        String queue4Ocid = service.createQueue('dev-mem1', retentionSeconds, visibilitySeconds, dlqDeliveryCount)
        String queue5Ocid = service.createQueue('dev-mem5', retentionSeconds, visibilitySeconds, dlqDeliveryCount)

        when:
        ListQueuesResponse queues = service.listQueues()

        then:
        queues.queueCollection.items.size() >= 5
        QueueSummary element = queues.queueCollection.items.find { it.id == queue4Ocid }
        int index = queues.queueCollection.items.indexOf(element)
        // ordering by display name
        queues.queueCollection.items[index].id == queue4Ocid
        queues.queueCollection.items[++index].id == queue2Ocid
        queues.queueCollection.items[++index].id == queue3Ocid
        queues.queueCollection.items[++index].id == queue1Ocid
        queues.queueCollection.items[++index].id == queue5Ocid

        cleanup:
        [queue1Ocid,
         queue2Ocid,
         queue3Ocid,
         queue4Ocid,
         queue5Ocid].each { String ocid -> service.deleteQueue(ocid, false) }
    }
}
