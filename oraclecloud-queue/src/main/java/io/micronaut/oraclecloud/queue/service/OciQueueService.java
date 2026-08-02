/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.queue.service;

import com.oracle.bmc.queue.Queue;
import com.oracle.bmc.queue.QueueAdmin;
import com.oracle.bmc.queue.model.CreateQueueDetails;
import com.oracle.bmc.queue.model.DeleteMessagesDetails;
import com.oracle.bmc.queue.model.DeleteMessagesDetailsEntry;
import com.oracle.bmc.queue.model.MessageMetadata;
import com.oracle.bmc.queue.model.PurgeQueueDetails;
import com.oracle.bmc.queue.model.PurgeQueueDetails.PurgeType;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.model.UpdateMessageDetails;
import com.oracle.bmc.queue.model.UpdateMessagesDetails;
import com.oracle.bmc.queue.model.UpdateMessagesDetailsEntry;
import com.oracle.bmc.queue.model.UpdateQueueDetails;
import com.oracle.bmc.queue.model.WorkRequest;
import com.oracle.bmc.queue.requests.CreateQueueRequest;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.DeleteMessagesRequest;
import com.oracle.bmc.queue.requests.DeleteQueueRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.requests.GetQueueRequest;
import com.oracle.bmc.queue.requests.GetStatsRequest;
import com.oracle.bmc.queue.requests.GetWorkRequestRequest;
import com.oracle.bmc.queue.requests.ListChannelsRequest;
import com.oracle.bmc.queue.requests.ListQueuesRequest;
import com.oracle.bmc.queue.requests.PurgeQueueRequest;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import com.oracle.bmc.queue.requests.UpdateMessageRequest;
import com.oracle.bmc.queue.requests.UpdateMessagesRequest;
import com.oracle.bmc.queue.requests.UpdateQueueRequest;
import com.oracle.bmc.queue.responses.DeleteMessageResponse;
import com.oracle.bmc.queue.responses.DeleteMessagesResponse;
import com.oracle.bmc.queue.responses.GetMessagesResponse;
import com.oracle.bmc.queue.responses.GetQueueResponse;
import com.oracle.bmc.queue.responses.GetStatsResponse;
import com.oracle.bmc.queue.responses.ListChannelsResponse;
import com.oracle.bmc.queue.responses.ListQueuesResponse;
import com.oracle.bmc.queue.responses.PutMessagesResponse;
import com.oracle.bmc.queue.responses.UpdateMessageResponse;
import com.oracle.bmc.queue.responses.UpdateMessagesResponse;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.messaging.exceptions.MessagingClientException;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.oracle.bmc.queue.model.OperationStatus.Failed;
import static com.oracle.bmc.queue.model.OperationStatus.Succeeded;
import static com.oracle.bmc.queue.model.SortOrder.Asc;
import static com.oracle.bmc.queue.requests.ListQueuesRequest.SortBy.DisplayName;
import static io.micronaut.oraclecloud.queue.advice.AbstractQueueIntroductionAdvice.MAX_GET_MESSAGES_LIMIT;

/**
 * OCI implementation of {@link QueueService}.
 */
@Singleton
public class OciQueueService implements QueueService {

    private static final Logger LOG = LoggerFactory.getLogger(OciQueueService.class);

    private final QueueAdmin adminClient;
    private final Queue client;
    private final String compartmentOcid;
    private final int maxWaitTries;
    private final int waitSleepMillis;
    private String endpoint;

    OciQueueService(QueueAdmin adminClient,
                    Queue client,
                    OracleQueueConfiguration config) {
        this.adminClient = adminClient;
        this.client = client;
        compartmentOcid = config.getCompartmentOcid();
        maxWaitTries = config.getAdminMaxWaitTries();
        waitSleepMillis = config.getAdminWaitSleepSeconds() * 1000;
    }

    @NonNull
    @Override
    public String createQueue(@NonNull String displayName,
                              @Nullable Integer retentionInSeconds,
                              @Nullable Integer visibilityInSeconds,
                              @Nullable Integer deadLetterQueueDeliveryCount) {
        return createQueue(compartmentOcid, displayName,
            retentionInSeconds, visibilityInSeconds, deadLetterQueueDeliveryCount);
    }

    @NonNull
    @Override
    public String createQueue(@NonNull String compartmentOcid,
                              @NonNull String displayName,
                              @Nullable Integer retentionInSeconds,
                              @Nullable Integer visibilityInSeconds,
                              @Nullable Integer deadLetterQueueDeliveryCount) {
        LOG.info("Creating queue with display name '{}' in compartment {}",
            displayName, compartmentOcid);

        var response = adminClient.createQueue(CreateQueueRequest.builder()
            .createQueueDetails(CreateQueueDetails.builder()
                .compartmentId(compartmentOcid)
                .displayName(displayName)
                .retentionInSeconds(retentionInSeconds)
                .visibilityInSeconds(visibilityInSeconds)
                .deadLetterQueueDeliveryCount(deadLetterQueueDeliveryCount)
                .build())
            .build());
        String jobId = response.getOpcWorkRequestId();
        LOG.debug("Queue creation job request id {}", jobId);

        WorkRequest workRequest = waitForJob(jobId, "create",
            "display name '" + displayName + "'");
        String ocid = workRequest.getResources().get(0).getIdentifier();
        LOG.info("Created queue with display name {}: {}", displayName, ocid);
        return ocid;
    }

    @NonNull
    @Override
    public String deleteQueue(@NonNull String ocid) {
        return deleteQueue(ocid, true);
    }

    @NonNull
    @Override
    public String deleteQueue(@NonNull String ocid, boolean waitForJob) {
        LOG.info("Deleting queue {}", ocid);

        var response = adminClient.deleteQueue(DeleteQueueRequest.builder()
            .queueId(ocid)
            .build());
        String jobId = response.getOpcWorkRequestId();
        LOG.debug("Queue deletion job request id {}", jobId);

        if (waitForJob) {
            waitForJob(jobId, "delete", "OCID " + ocid);
            LOG.info("Queue {} deleted", ocid);
        }

        return jobId;
    }

    @NonNull
    @Override
    public String purgeQueue(@NonNull String ocid,
                             @Nullable String channel,
                             boolean purgeDlqOnly) {
        return purgeQueue(ocid, channel, purgeDlqOnly, true);
    }

    @NonNull
    @Override
    public String purgeQueue(@NonNull String ocid,
                             @Nullable String channel,
                             boolean purgeDlqOnly,
                             boolean waitForJob) {
        LOG.info("Purging queue {} (channel: {})", ocid, name(channel));

        var response = adminClient.purgeQueue(PurgeQueueRequest.builder()
            .queueId(ocid)
            .purgeQueueDetails(PurgeQueueDetails.builder()
                .purgeType(purgeDlqOnly ? PurgeType.Dlq : PurgeType.Both)
                .channelIds(channel == null ? null : List.of(channel))
                .build())
            .build());

        String jobId = response.getOpcWorkRequestId();
        LOG.debug("Queue purge job request id {}", jobId);

        if (waitForJob) {
            waitForJob(jobId, "purge", "OCID " + ocid);
            LOG.info("Queue {} purged", ocid);
        }

        return jobId;
    }

    @NonNull
    @Override
    public GetQueueResponse getQueue(@NonNull String ocid) {
        LOG.info("Getting queue {}", ocid);

        return adminClient.getQueue(GetQueueRequest.builder()
            .queueId(ocid)
            .build());
    }

    @Override
    public void updateQueue(@NonNull String ocid,
                            @Nullable Integer visibilityInSeconds,
                            @Nullable Integer deadLetterQueueDeliveryCount) {
        LOG.info("Updating queue {}", ocid);

        var response = adminClient.updateQueue(UpdateQueueRequest.builder()
            .queueId(ocid)
            .updateQueueDetails(UpdateQueueDetails.builder()
                .visibilityInSeconds(visibilityInSeconds)
                .deadLetterQueueDeliveryCount(deadLetterQueueDeliveryCount)
                .build())
            .build());

        String jobId = response.getOpcWorkRequestId();
        LOG.debug("Queue update job request id {}", jobId);

        waitForJob(jobId, "update", "OCID " + ocid);
        LOG.info("Queue {} updated", ocid);
    }

    @NonNull
    @Override
    public ListQueuesResponse listQueues() {
        return listQueues(compartmentOcid, null);
    }

    @NonNull
    @Override
    public ListQueuesResponse listQueues(@NonNull String compartmentOcid) {
        return listQueues(compartmentOcid, null);
    }

    @NonNull
    @Override
    public ListQueuesResponse listQueues(@NonNull String compartmentOcid,
                                         @Nullable String page) {
        LOG.info("Listing queues in compartment {}", compartmentOcid);

        return adminClient.listQueues(ListQueuesRequest.builder()
            .compartmentId(compartmentOcid)
            .limit(1000)
            .page(page)
            .sortBy(DisplayName)
            .sortOrder(Asc)
            .build());
    }

    @NonNull
    @Override
    public WorkRequest lookupWorkRequest(@NonNull String jobId) {
        return adminClient.getWorkRequest(GetWorkRequestRequest.builder()
                .workRequestId(jobId)
                .build())
            .getWorkRequest();
    }

    @NonNull
    @Override
    public UpdateMessageResponse updateMessage(@NonNull String queueOcid,
                                               @NonNull String messageReceipt,
                                               int extensionSeconds) {
        LOG.info("Extend processing of message with receipt {} on queue {}",
            messageReceipt, queueOcid);

        return getClient(queueOcid).updateMessage(UpdateMessageRequest.builder()
            .queueId(queueOcid)
            .messageReceipt(messageReceipt)
            .updateMessageDetails(UpdateMessageDetails.builder()
                .visibilityInSeconds(extensionSeconds)
                .build())
            .build());
    }

    @NonNull
    @Override
    public UpdateMessagesResponse updateMessages(@NonNull String queueOcid,
                                                 @NonNull List<String> messageReceipts,
                                                 int extensionSeconds) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Bulk extending processing of messages with receipts {} on queue {}",
                String.join(",", messageReceipts), queueOcid);
        }

        List<UpdateMessagesDetailsEntry> entries = messageReceipts.stream().map(r ->
                UpdateMessagesDetailsEntry.builder()
                    .receipt(r)
                    .visibilityInSeconds(extensionSeconds)
                    .build())
            .toList();

        var response = getClient(queueOcid).updateMessages(UpdateMessagesRequest.builder()
            .queueId(queueOcid)
            .updateMessagesDetails(UpdateMessagesDetails.builder()
                .entries(entries)
                .build())
            .build());

        Integer serverFailures = response.getUpdateMessagesResult().getServerFailures();
        if (serverFailures != null && serverFailures > 0) {
            LOG.warn("Not all messages could be updated: {}", response.getUpdateMessagesResult());
        }

        return response;
    }

    @NonNull
    @Override
    public DeleteMessageResponse deleteMessage(@NonNull String queueOcid,
                                               @NonNull String messageReceipt) {
        LOG.info("Deleting message with receipt {} on queue {}", messageReceipt, queueOcid);

        return getClient(queueOcid).deleteMessage(DeleteMessageRequest.builder()
            .queueId(queueOcid)
            .messageReceipt(messageReceipt)
            .build());
    }

    @NonNull
    @Override
    public DeleteMessagesResponse deleteMessages(@NonNull String queueOcid,
                                                 @NonNull List<String> messageReceipts) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Bulk deleting messages with receipts {} on queue {}",
                String.join(",", messageReceipts), queueOcid);
        }

        List<DeleteMessagesDetailsEntry> entries = messageReceipts.stream()
            .map(r -> DeleteMessagesDetailsEntry.builder().receipt(r).build())
            .toList();

        var response = getClient(queueOcid).deleteMessages(DeleteMessagesRequest.builder()
            .queueId(queueOcid)
            .deleteMessagesDetails(DeleteMessagesDetails.builder()
                .entries(entries)
                .build())
            .build());

        Integer serverFailures = response.getDeleteMessagesResult().getServerFailures();

        // The returned value is a string representation of a number of errors that occurred during deletion
        if (serverFailures != null && serverFailures > 0) {
            LOG.warn("Not all messages could be deleted: {}",
                response.getDeleteMessagesResult());
        }

        return response;
    }

    @NonNull
    @Override
    public PutMessagesResponse putMessage(@NonNull String queueOcid,
                                          @Nullable String channel,
                                          @NonNull String message,
                                          @Nullable Map<String, String> metadata) {
        return putMessages(queueOcid, channel, List.of(message),
            metadata == null ? null : List.of(metadata));
    }

    @NonNull
    @Override
    public PutMessagesResponse putMessages(@NonNull String queueOcid,
                                           @Nullable String channel,
                                           @NonNull List<String> messages,
                                           @Nullable List<Map<String, String>> metadata) {
        int msgCount = messages.size();
        boolean hasMetadata = metadata != null && !metadata.isEmpty();
        if (hasMetadata && metadata.size() != msgCount) {
            throw new MessagingClientException("putMessage messages list size must match metadata list size: " +
                msgCount + "!=" + metadata.size());
        }

        if (msgCount == 1) {
            LOG.debug("Putting message {} on queue {} (channel: {})",
                messages.get(0), queueOcid, name(channel));
        } else if (msgCount > 1) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Bulk putting messages {} on queue {} (channel: {})",
                    String.join(",", messages), queueOcid, name(channel));
            }
        }

        List<PutMessagesDetailsEntry> entries = new ArrayList<>();
        for (int i = 0, count = messages.size(); i < count; i++) {
            if (channel != null || hasMetadata) {
                entries.add(PutMessagesDetailsEntry.builder()
                    .content(messages.get(i))
                    .metadata(MessageMetadata.builder()
                        .channelId(channel)
                        .customProperties(hasMetadata ? metadata.get(i) : null)
                        .build())
                    .build());
            } else {
                entries.add(PutMessagesDetailsEntry.builder()
                    .content(messages.get(i))
                    .build());
            }
        }

        return getClient(queueOcid).putMessages(PutMessagesRequest.builder()
            .queueId(queueOcid)
            .putMessagesDetails(PutMessagesDetails.builder().messages(entries).build())
            .build());
    }

    @NonNull
    @Override
    public GetMessagesResponse getMessage(@NonNull String queueOcid,
                                          @Nullable String channelFilter) {
        return getMessages(null, queueOcid, channelFilter, 0, 1);
    }

    @NonNull
    @Override
    public GetMessagesResponse getMessages(@NonNull String queueOcid,
                                           @Nullable String channelFilter) {
        return getMessages(null, queueOcid, channelFilter, 0, MAX_GET_MESSAGES_LIMIT);
    }

    @NonNull
    @Override
    public GetMessagesResponse getMessages(@NonNull String queueOcid,
                                           @Nullable String channelFilter,
                                           @Max(20) int limit) {
        return getMessages(null, queueOcid, channelFilter, 0, limit);
    }

    @NonNull
    @Override
    public GetMessagesResponse getMessages(@NonNull String queueOcid,
                                           @Nullable String channelFilter,
                                           @Max(20) int limit,
                                           int visibilityInSeconds) {
        return getMessages(visibilityInSeconds, queueOcid, channelFilter, 0, limit);
    }

    @NonNull
    @Override
    public GetMessagesResponse waitMessage(@NonNull String queueOcid,
                                           @Nullable String channelFilter,
                                           @Min(5) @Max(30) int timeout) {
        return getMessages(null, queueOcid, channelFilter, timeout, 1);
    }

    @NonNull
    @Override
    public GetMessagesResponse waitMessages(@NonNull String queueOcid,
                                            @Nullable String channelFilter,
                                            @Min(5) @Max(30) int timeout) {
        return getMessages(null, queueOcid, channelFilter, timeout,
            MAX_GET_MESSAGES_LIMIT);
    }

    @NonNull
    @Override
    public GetMessagesResponse waitMessages(@NonNull String queueOcid,
                                            @Nullable String channelFilter,
                                            @Min(5) @Max(30) int timeout,
                                            @Max(20) int limit) {
        return getMessages(null, queueOcid, channelFilter, timeout, limit);
    }

    @NonNull
    @Override
    public GetMessagesResponse waitMessages(@NonNull String queueOcid,
                                            @Nullable String channelFilter,
                                            @Min(5) @Max(30) int timeout,
                                            @Max(20) int limit,
                                            int visibilityInSeconds) {
        return getMessages(visibilityInSeconds, queueOcid, channelFilter, timeout, limit);
    }

    private GetMessagesResponse getMessages(Integer visibilityInSeconds,
                                            String queueOcid,
                                            String channelFilter,
                                            int timeout,
                                            int limit) {
        LOG.debug("{} polling on queue {} (channel: {})",
            timeout == 0 ? "Short" : "Long", queueOcid, name(channelFilter));

        return getClient(queueOcid).getMessages(GetMessagesRequest.builder()
            .queueId(queueOcid)
            .limit(limit)
            .timeoutInSeconds(timeout)
            .visibilityInSeconds(visibilityInSeconds)
            .channelFilter(channelFilter)
            .build());
    }

    @NonNull
    @Override
    public ListChannelsResponse listChannels(@NonNull String queueOcid,
                                             @Nullable String channelFilter) {
        return listChannels(queueOcid, channelFilter, null);
    }

    @NonNull
    @Override
    public ListChannelsResponse listChannels(@NonNull String queueOcid,
                                             @Nullable String channelFilter,
                                             @Nullable String page) {
        LOG.info("Attempting to list channels for queue {}", queueOcid);

        return getClient(queueOcid).listChannels(ListChannelsRequest.builder()
            .queueId(queueOcid)
            .limit(1000)
            .page(page)
            .channelFilter(channelFilter)
            .build());
    }

    @NonNull
    @Override
    public GetStatsResponse getStats(@NonNull String queueOcid) {
        LOG.info("Obtaining queue stats for queue {}", queueOcid);

        return getClient(queueOcid).getStats(GetStatsRequest.builder()
            .queueId(queueOcid)
            .build());
    }

    private WorkRequest waitForJob(String jobId,
                                   String action,
                                   String exceptionInfo) {
        for (int i = 0; i < maxWaitTries; i++) {
            var workRequest = lookupWorkRequest(jobId);
            LOG.debug("Queue " + action + " job request status: {}", workRequest.getStatus());

            if (workRequest.getStatus() == Succeeded) {
                return workRequest;
            }

            if (workRequest.getStatus() == Failed) {
                throw new MessagingClientException(
                    "Queue " + action + " (" + exceptionInfo + ") failed : " + workRequest);
            }

            try {
                Thread.sleep(waitSleepMillis);
            } catch (InterruptedException ignored) {
                // ignored
            }
        }

        throw new MessagingClientException(
            "Queue " + action + " (" + exceptionInfo + ") " +
                "did not complete with status Succeeded or Failed after " +
                maxWaitTries * waitSleepMillis + "ms");
    }

    private static String name(String s) {
        return s == null ? "n/a" : s;
    }

    /**
     * Because of <a href="https://github.com/oracle/oci-java-sdk/issues/666">this bug</a>, the
     * initially constructed client will have the wrong endpoint set, e.g.
     * https://messaging.us-phoenix-1.oci.oraclecloud.com, but it should be
     * https://cell-1.queue.messaging.us-phoenix-1.oci.oraclecloud.com. The docs say that each
     * queue's endpoint can be different. However, in practice the endpoints are generally (always?)
     * the same per region. We only support a single region, so we override here and expect that
     * after the first update, the values will always be the same, so this will be thread-safe.
     * <p>
     * We can't do this reliably at startup, since this service can be used independently of the
     * queues configured in the config, so we don't know which queues will be used.
     * <p>
     * The method is protected to allow subclassing to change behavior, e.g. to maintain a pool of
     * clients with different regions and/or endpoints.
     *
     * @param queueOcid the queue OCID
     */
    protected Queue getClient(String queueOcid) {
        if (endpoint == null) {
            // not thread-safe, but concurrent calls will return the same value
            endpoint = getQueue(queueOcid).getQueue().getMessagesEndpoint();
            client.setEndpoint(endpoint);
        }

        return client;
    }
}
