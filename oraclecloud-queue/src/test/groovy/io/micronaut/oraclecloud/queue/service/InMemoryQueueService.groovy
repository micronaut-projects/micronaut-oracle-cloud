package io.micronaut.oraclecloud.queue.service

import com.oracle.bmc.queue.model.ChannelCollection
import com.oracle.bmc.queue.model.DeleteMessagesResult
import com.oracle.bmc.queue.model.GetMessage
import com.oracle.bmc.queue.model.GetMessages
import com.oracle.bmc.queue.model.MessageMetadata
import com.oracle.bmc.queue.model.PutMessage
import com.oracle.bmc.queue.model.PutMessages
import com.oracle.bmc.queue.model.Queue
import com.oracle.bmc.queue.model.QueueCollection
import com.oracle.bmc.queue.model.QueueStats
import com.oracle.bmc.queue.model.QueueSummary
import com.oracle.bmc.queue.model.Stats
import com.oracle.bmc.queue.model.UpdateMessagesResult
import com.oracle.bmc.queue.model.UpdatedMessage
import com.oracle.bmc.queue.model.WorkRequest
import com.oracle.bmc.queue.responses.DeleteMessageResponse
import com.oracle.bmc.queue.responses.DeleteMessagesResponse
import com.oracle.bmc.queue.responses.GetMessagesResponse
import com.oracle.bmc.queue.responses.GetQueueResponse
import com.oracle.bmc.queue.responses.GetStatsResponse
import com.oracle.bmc.queue.responses.ListChannelsResponse
import com.oracle.bmc.queue.responses.ListQueuesResponse
import com.oracle.bmc.queue.responses.PutMessagesResponse
import com.oracle.bmc.queue.responses.UpdateMessageResponse
import com.oracle.bmc.queue.responses.UpdateMessagesResponse
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration.QueueConfig
import jakarta.inject.Singleton
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Predicate

import static com.oracle.bmc.queue.model.OperationStatus.Succeeded
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

@Slf4j('LOG')
@Replaces(QueueService)
@Singleton
@CompileStatic
class InMemoryQueueService implements QueueService {

    private final Map<String, Queue> queues = new ConcurrentHashMap<>()
    private final Map<String, List<GetMessage>> queueMessages = new ConcurrentHashMap<>()
    private final AtomicLong id = new AtomicLong(10000)

    private final String compartmentOcid

    InMemoryQueueService(OracleQueueConfiguration config) {
        compartmentOcid = config.compartmentOcid

        for (QueueConfig queueConfig in config.queues) {
            queues[queueConfig.ocid] = buildQueue(queueConfig.ocid, queueConfig.ocid)
        }
    }

    @Override
    String createQueue(String displayName,
                       Integer retentionInSeconds,
                       Integer visibilityInSeconds,
                       Integer deadLetterQueueDeliveryCount) {
        createQueue compartmentOcid, displayName, retentionInSeconds,
                visibilityInSeconds, deadLetterQueueDeliveryCount
    }

    @Override
    String createQueue(String compartmentOcid,
                       String displayName,
                       Integer retentionInSeconds,
                       Integer visibilityInSeconds,
                       Integer deadLetterQueueDeliveryCount) {
        LOG.info 'Creating queue with display name {} in compartment {}',
                displayName, compartmentOcid

        String ocid = 'ocid1.queue.oc1.in.memory.' + new Random().nextInt(Integer.MAX_VALUE)
        queues[ocid] = buildQueue(ocid, displayName, retentionInSeconds,
                visibilityInSeconds, deadLetterQueueDeliveryCount)
        return ocid
    }

    @Override
    String deleteQueue(String ocid) {
        deleteQueue ocid, true
    }

    @Override
    String deleteQueue(String ocid, boolean waitForJob) {
        LOG.info 'Deleting queue {}', ocid

        queues.remove ocid
        queueMessages.remove ocid
        return 'ocid1.workrequest.oc1.in.memory.123'
    }

    @Override
    String purgeQueue(String ocid,
                      String channel,
                      boolean purgeDlqOnly) {
        purgeQueue ocid, channel, purgeDlqOnly, true
    }

    @Override
    String purgeQueue(String ocid,
                      String channel,
                      boolean purgeDlqOnly,
                      boolean waitForJob) {
        LOG.info 'Purging queue {} (channel: {})', ocid, name(channel)

        createQueueUsingDefaultsIfMissing ocid

        List<GetMessage> messages = queueMessages(ocid)
        if (channel == null) {
            messages.clear()
        } else {
            messages.removeIf(it -> channel == it?.metadata?.channelId)
        }

        return 'ocid1.workrequest.oc1.in.memory.123'
    }

    @Override
    GetQueueResponse getQueue(String ocid) {
        LOG.info 'Get queue {}', ocid

        createQueueUsingDefaultsIfMissing ocid

        return GetQueueResponse.builder()
                .queue(queues[ocid])
                .__httpStatusCode__(OK.code)
                .build()
    }

    @Override
    void updateQueue(String ocid,
                     Integer visibilityInSeconds,
                     Integer deadLetterQueueDeliveryCount) {
        LOG.info 'Update queue {}', ocid

        createQueueUsingDefaultsIfMissing ocid

        Queue queue = queues[ocid]
        if (!queue) {
            return
        }

        queues.put(ocid, queue.toBuilder()
                .visibilityInSeconds(visibilityInSeconds)
                .deadLetterQueueDeliveryCount(deadLetterQueueDeliveryCount)
                .timeUpdated(new Date())
                .build())
    }

    @Override
    ListQueuesResponse listQueues(String compartmentOcid = compartmentOcid,
                                  String page = null) {
        LOG.info 'List queues in compartment {}', compartmentOcid

        List<QueueSummary> items = queues.values()
                .stream()
                .sorted(Comparator.comparing(Queue::getDisplayName))
                .map(q -> QueueSummary.builder()
                        .id(q.id)
                        .displayName(q.displayName)
                        .compartmentId(q.compartmentId)
                        .timeCreated(q.timeCreated)
                        .timeUpdated(q.timeUpdated)
                        .build())
                .toList()
        return ListQueuesResponse.builder()
                .queueCollection(QueueCollection.builder().items(items).build())
                .__httpStatusCode__(OK.code)
                .build()
    }

    @Override
    WorkRequest lookupWorkRequest(String jobId) {
        return WorkRequest.builder()
                .id(jobId)
                .status(Succeeded)
                .percentComplete(100f)
                .timeFinished(new Date())
                .build()
    }

    @Override
    UpdateMessageResponse updateMessage(String queueOcid,
                                        String messageReceipt,
                                        int extensionSeconds) {
        LOG.info 'Extend processing of message with receipt {} on queue {}',
                messageReceipt, queueOcid

        createQueueUsingDefaultsIfMissing queueOcid

        List<GetMessage> msgs = queueMessages(queueOcid)
        GetMessage updated = null
        Optional<GetMessage> existing = msgs.stream()
                .filter(m -> m.receipt == messageReceipt)
                .findFirst()
        if (existing) {
            GetMessage message = existing.get()
            updated = message.toBuilder()
                    .visibleAfter(new Date(System.currentTimeMillis() + 1000 * extensionSeconds))
                    .build()
            msgs.remove(message)
            msgs << updated
        }

        if (!updated) {
            return UpdateMessageResponse.builder()
                    .__httpStatusCode__(NOT_FOUND.code)
                    .build()
        }

        return UpdateMessageResponse.builder()
                .updatedMessage(UpdatedMessage.builder()
                        .id(updated.id)
                        .visibleAfter(updated.visibleAfter)
                        .build())
                .__httpStatusCode__(OK.code)
                .build()
    }

    @Override
    UpdateMessagesResponse updateMessages(String queueOcid,
                                          List<String> messageReceipts,
                                          int extensionSeconds) {
        if (LOG.infoEnabled) {
            LOG.info 'Bulk extending processing of messages with receipts {} on queue {}',
                    String.join(',', messageReceipts), queueOcid
        }

        createQueueUsingDefaultsIfMissing queueOcid

        List<GetMessage> msgs = queueMessages(queueOcid)

        int failures = 0
        for (String messageReceipt in messageReceipts) {
            Optional<GetMessage> existing = msgs.stream()
                    .filter(m -> m.receipt == messageReceipt)
                    .findFirst()
            if (!existing) {
                failures++
                continue
            }

            GetMessage message = existing.get()
            GetMessage updated = message.toBuilder()
                    .visibleAfter(new Date(System.currentTimeMillis() + 1000 * extensionSeconds))
                    .build()
            msgs.remove message
            msgs << updated
        }

        return UpdateMessagesResponse.builder()
                .updateMessagesResult(UpdateMessagesResult.builder()
                        .clientFailures(0)
                        .serverFailures(failures)
                        .build())
                .__httpStatusCode__(OK.code)
                .build()
    }

    @Override
    DeleteMessageResponse deleteMessage(String queueOcid, String messageReceipt) {
        LOG.info 'Deleting message with receipt {} on queue {}',
                messageReceipt, queueOcid

        createQueueUsingDefaultsIfMissing queueOcid

        List<GetMessage> msgs = queueMessages(queueOcid)
        msgs.removeIf(e -> e.receipt == messageReceipt)

        return DeleteMessageResponse.builder().__httpStatusCode__(204).build()
    }

    @Override
    DeleteMessagesResponse deleteMessages(String queueOcid, List<String> messageReceipts) {
        if (LOG.infoEnabled) {
            LOG.info 'Bulk deleting messages with receipts {} on queue {}',
                    String.join(',', messageReceipts), queueOcid
        }

        createQueueUsingDefaultsIfMissing queueOcid

        List<GetMessage> msgs = queueMessages(queueOcid)
        int failures = 0
        for (String messageReceipt in messageReceipts) {
            boolean removed = msgs.removeIf(it -> it.receipt == messageReceipt)
            if (!removed) {
                failures++
            }
        }

        return DeleteMessagesResponse.builder()
                .deleteMessagesResult(DeleteMessagesResult.builder()
                        .clientFailures(0)
                        .serverFailures(failures)
                        .build())
                .__httpStatusCode__(OK.code)
                .build()
    }

    @Override
    PutMessagesResponse putMessage(String queueOcid,
                                   String channel,
                                   String message,
                                   Map<String, String> metadata) {
        putMessages queueOcid, channel, List.of(message),
                metadata == null ? null : List.of(metadata)
    }

    @Override
    PutMessagesResponse putMessages(String queueOcid,
                                    String channel,
                                    List<String> messages,
                                    List<Map<String, String>> metadata) {

        int msgCount = messages.size()
        boolean hasMetadata = metadata
        if (hasMetadata && metadata.size() != msgCount) {
            throw new IllegalArgumentException('putMessage messages list size must match metadata list size: ' +
                    msgCount + '!=' + metadata.size())
        }

        if (msgCount == 1) {
            LOG.debug 'Putting message {} on queue {} (channel: {})',
                    messages[0], queueOcid, name(channel)
        } else if (msgCount > 1) {
            if (LOG.debugEnabled) {
                LOG.debug 'Bulk putting messages {} on queue {} (channel: {})',
                        String.join(',', messages), queueOcid, name(channel)
            }
        }

        createQueueUsingDefaultsIfMissing queueOcid

        Date expiry = new Date(System.currentTimeMillis() +
                queues[queueOcid].retentionInSeconds * 1000)

        List<GetMessage> msgs = []
        for (int i = 0; i < messages.size(); i++) {

            var builder = GetMessage.builder()
                    .content(messages[i])
                    .id(id.incrementAndGet())
                    .deliveryCount(0)
                    .visibleAfter(new Date())
                    .expireAfter(expiry)
                    .receipt(UUID.randomUUID().toString())

            if (channel || hasMetadata) {
                builder.metadata(MessageMetadata.builder()
                        .channelId(channel)
                        .customProperties(hasMetadata ? metadata[i] : null)
                        .build())
            }

            msgs << builder.build()
        }
        queueMessages(queueOcid).addAll msgs

        return PutMessagesResponse.builder()
                .putMessages(PutMessages.builder()
                        .messages(msgs.stream()
                                .map(m -> PutMessage.builder().id(m.id).build())
                                .toList())
                        .build())
                .__httpStatusCode__(OK.code)
                .build()
    }

    @Override
    GetMessagesResponse getMessage(String queueOcid, String channelFilter) {
        LOG.info 'Short polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, 1, -1)
    }

    @Override
    GetMessagesResponse getMessages(String queueOcid, String channelFilter) {
        LOG.info 'Short polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, 20, -1)
    }

    @Override
    GetMessagesResponse getMessages(String queueOcid,
                                    String channelFilter,
                                    int limit) {
        LOG.info 'Short polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, limit, -1)
    }

    @Override
    GetMessagesResponse getMessages(String queueOcid,
                                    String channelFilter,
                                    int limit,
                                    int visibilityInSeconds) {
        LOG.info 'Short polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, limit, visibilityInSeconds)
    }

    @Override
    GetMessagesResponse waitMessage(String queueOcid,
                                    String channelFilter,
                                    @Min(5) @Max(30) int timeout) {
        LOG.info 'Long polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, 1, -1)
    }

    @Override
    GetMessagesResponse waitMessages(String queueOcid,
                                     String channelFilter,
                                     @Min(5) @Max(30) int timeout) {
        LOG.info 'Long polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, 20, -1)
    }

    @Override
    GetMessagesResponse waitMessages(String queueOcid,
                                     String channelFilter,
                                     @Min(5) @Max(30) int timeout,
                                     int limit) {
        LOG.info 'Long polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, limit, -1)
    }

    @Override
    GetMessagesResponse waitMessages(String queueOcid,
                                     String channelFilter,
                                     @Min(5) @Max(30) int timeout,
                                     int limit,
                                     int visibilityInSeconds) {
        LOG.info 'Long polling on queue {} (channel: {})',
                queueOcid, name(channelFilter)

        return getMessagesHelper(queueOcid, channelFilter, limit, visibilityInSeconds)
    }

    protected GetMessagesResponse getMessagesHelper(String queueOcid,
                                                    String channelFilter,
                                                    int limit,
                                                    int visibilityInSeconds) {
        createQueueUsingDefaultsIfMissing queueOcid

        synchronized (this) {
            Date now = new Date()
            Date lockedTo = new Date(now.time +
                    (visibilityInSeconds < 0 ? queues[queueOcid].visibilityInSeconds : visibilityInSeconds) * 1000)
            List<GetMessage> messages = queueMessages(queueOcid)
            List<GetMessage> filtered = messages.stream()
                    .filter(m -> channelFilter == null || m.metadata && channelFilter == m.metadata.channelId)
                    .filter(m -> m.visibleAfter.before(now))
                    .limit(limit)
                    .toList()
            if (filtered) {
                List<GetMessage> updated = filtered.stream()
                        .map(m -> m.toBuilder()
                                .visibleAfter(lockedTo)
                                .deliveryCount(m.deliveryCount + 1)
                                .build())
                        .toList()
                messages.removeAll filtered
                messages.addAll updated
                filtered = updated
            }

            return GetMessagesResponse.builder()
                    .getMessages(GetMessages.builder().messages(filtered).build())
                    .__httpStatusCode__(OK.code)
                    .build()
        }
    }

    @Override
    ListChannelsResponse listChannels(String queueOcid,
                                      String page = null,
                                      String channelFilter) {
        LOG.info 'Listing channels for queue {}', queueOcid

        List<String> channels = queueMessages(queueOcid)
                .stream()
                .map(m -> m.metadata == null ? null : m.metadata.channelId)
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isEmpty))
                .filter(channel -> channelFilter == null || channelFilter == channel)
                .distinct()
                .toList()
        return ListChannelsResponse.builder()
                .channelCollection(ChannelCollection.builder().items(channels).build())
                .__httpStatusCode__(OK.code)
                .build()
    }

    @Override
    GetStatsResponse getStats(String queueOcid) {
        LOG.info 'Obtaining queue stats for queue {}', queueOcid

        createQueueUsingDefaultsIfMissing queueOcid

        return GetStatsResponse.builder()
                .__httpStatusCode__(OK.code)
                .queueStats(QueueStats.builder()
                        .queue(Stats.builder()
                                .visibleMessages((long) queueMessages(queueOcid).size())
                                .build())
                        .build())
                .build()
    }

    private static String name(String s) {
        s ?: 'n/a'
    }

    List<GetMessage> queueMessages(String ocid) {
        queueMessages.computeIfAbsent(ocid, k -> [])
    }

    private synchronized void createQueueUsingDefaultsIfMissing(String ocid) {
        if (!queues.containsKey(ocid)) {
            queues[ocid] = buildQueue(ocid, 'queue-' + new Random().nextInt(10000),
                    null, null, 3)
        }
    }

    private Queue buildQueue(String ocid,
                             String displayName,
                             Integer retentionInSeconds = null,
                             Integer visibilityInSeconds = null,
                             Integer deadLetterQueueDeliveryCount = null) {
        Queue.builder()
                .id(ocid)
                .compartmentId(compartmentOcid)
                .displayName(displayName)
                .retentionInSeconds(retentionInSeconds == null ? 86400 : retentionInSeconds)
                .visibilityInSeconds(visibilityInSeconds == null ? 30 : visibilityInSeconds)
                .deadLetterQueueDeliveryCount(deadLetterQueueDeliveryCount)
                .timeCreated(new Date())
                .timeUpdated(new Date())
                .build()
    }
}
