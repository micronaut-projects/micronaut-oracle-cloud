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

import com.oracle.bmc.queue.model.WorkRequest;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Map;

/**
 * Interface for interacting with Oracle Cloud Queue.
 */
public interface QueueService {

    /**
     * Create a new queue in the default compartment as provided in config.
     *
     * @param displayName                  the display name of the queue
     * @param retentionInSeconds           the length of time that a queue retains a message until
     *                                     the system automatically deletes the message, if not
     *                                     deleted by a consumer. The default value is 1 day, and
     *                                     the value is configurable to values of 10 seconds to 7
     *                                     days at the queue level.
     * @param visibilityInSeconds          the length of time during which a message received from
     *                                     the queue by one consumer isn't visible to other
     *                                     consumers. The default value is 30 seconds, and the value
     *                                     is configurable to values of 1 second to 12 hours at the
     *                                     queue level. Consumers can specify the value when
     *                                     requesting messages.
     * @param deadLetterQueueDeliveryCount the number of times that a message is delivered to a
     *                                     consumer, but not updated or deleted, before it's sent to
     *                                     a dead letter queue (DLQ). The default value is 5, and
     *                                     the value is configurable at the queue level
     * @return the OCID of the new queue
     */
    @NonNull
    String createQueue(@NonNull String displayName,
                       @Nullable Integer retentionInSeconds,
                       @Nullable Integer visibilityInSeconds,
                       @Nullable Integer deadLetterQueueDeliveryCount);

    /**
     * Create a new queue.
     *
     * @param compartmentOcid              the compartment the queue will be created in
     * @param displayName                  the display name of the queue
     * @param retentionInSeconds           the length of time that a queue retains a message until
     *                                     the system automatically deletes the message, if not
     *                                     deleted by a consumer. The default value is 1 day, and
     *                                     the value is configurable to values of 10 seconds to 7
     *                                     days at the queue level.
     * @param visibilityInSeconds          the length of time during which a message received from
     *                                     the queue by one consumer isn't visible to other
     *                                     consumers. The default value is 30 seconds, and the value
     *                                     is configurable to values of 1 second to 12 hours at the
     *                                     queue level. Consumers can specify the value when
     *                                     requesting messages.
     * @param deadLetterQueueDeliveryCount the number of times that a message is delivered to a
     *                                     consumer, but not updated or deleted, before it's sent to
     *                                     a dead letter queue (DLQ). The default value is 5, and
     *                                     the value is configurable at the queue level
     * @return the OCID of the new queue
     */
    @NonNull
    String createQueue(@NonNull String compartmentOcid,
                       @NonNull String displayName,
                       @Nullable Integer retentionInSeconds,
                       @Nullable Integer visibilityInSeconds,
                       @Nullable Integer deadLetterQueueDeliveryCount);

    /**
     * Delete a queue and wait for the operation to complete.
     *
     * @param ocid the OCID of the queue
     * @return a work request job id that can be queried using the lookupWorkRequest method
     */
    @NonNull
    String deleteQueue(@NonNull String ocid);

    /**
     * Delete a queue, optionally waiting for the job to complete.
     *
     * @param ocid       the OCID of the queue
     * @param waitForJob whether to wait for the job to complete
     * @return a work request job id that can be queried using the lookupWorkRequest method
     */
    @NonNull
    String deleteQueue(@NonNull String ocid,
                       boolean waitForJob);

    /**
     * Purge a queue (remove messages) and wait for the operation to complete.
     *
     * @param ocid         the OCID of the queue
     * @param channel      optional channel id to just purge messages from the specified channel,
     *                     rather than all messages in the queue
     * @param purgeDlqOnly when true, only the DLQ is purged; otherwise both queue and DLQ are purged
     * @return a work request job id that can be queried using the lookupWorkRequest method
     */
    @NonNull
    String purgeQueue(@NonNull String ocid,
                      @Nullable String channel,
                      boolean purgeDlqOnly);

    /**
     * Purge a queue (remove messages) optionally waiting for the job to complete.
     *
     * @param ocid         the OCID of the queue
     * @param channel      optional channel id to just purge messages from the specified channel,
     *                     rather than all messages in the queue
     * @param purgeDlqOnly when true, only DLQ is purged; otherwise both queue and DLQ are purged
     * @param waitForJob   whether to wait for the job to complete
     * @return a work request job id that can be queried using the lookupWorkRequest method
     */
    @NonNull
    String purgeQueue(@NonNull String ocid,
                      @Nullable String channel,
                      boolean purgeDlqOnly,
                      boolean waitForJob);

    /**
     * Lookup a queue.
     *
     * @param ocid the OCID of the queue
     * @return queue configuration info
     */
    @NonNull
    GetQueueResponse getQueue(@NonNull String ocid);

    /**
     * Update configuration properties of a queue.
     *
     * @param ocid                         the OCID of the queue
     * @param visibilityInSeconds          the length of time during which a message received from
     *                                     the queue by one consumer isn't visible to other
     *                                     consumers. Visibility timeout is configurable to values
     *                                     of 1 second to 12 hours at the queue level, and
     *                                     consumers can set the value when requesting messages.
     * @param deadLetterQueueDeliveryCount the number of times that a message is delivered to a
     *                                     consumer, but not updated or deleted, before it's sent
     *                                     to a dead letter queue (DLQ). The maximum number of
     *                                     delivery attempts is configurable at the queue level
     */
    void updateQueue(@NonNull String ocid,
                     @Nullable Integer visibilityInSeconds,
                     @Nullable Integer deadLetterQueueDeliveryCount);

    /**
     * List queues in the default compartment.
     *
     * @return the queues
     */
    @NonNull
    ListQueuesResponse listQueues();

    /**
     * List queues in the specified compartment.
     *
     * @param compartmentOcid the compartment OCID
     * @return the queues
     */
    @NonNull
    ListQueuesResponse listQueues(@NonNull String compartmentOcid);

    /**
     * List queues in the specified compartment starting from the specified pagination value.
     *
     * @param compartmentOcid the compartment OCID
     * @param page            the pagination value
     * @return the queues
     */
    @NonNull
    ListQueuesResponse listQueues(@NonNull String compartmentOcid,
                                  @Nullable String page);

    /**
     * Retrieve a work request by id.
     *
     * @param jobId the work request job id
     * @return work request job status and details
     */
    @NonNull
    WorkRequest lookupWorkRequest(@NonNull String jobId);

    /**
     * Extend the visibility timeout of a message. If processing a message takes longer than
     * expected, consumers can extend the visibility timeout of a message. Extending the timeout
     * prevents the message from being returned to the queue and being delivered to another
     * consumer.
     *
     * @param queueOcid        the OCID of the queue
     * @param messageReceipt   an opaque token uniquely representing the message
     * @param extensionSeconds the new visibility of the message relative to the current time.
     *                         If the message is not explicitly deleted, it will become visible to
     *                         other consumers once this timeout is exhausted
     * @return updated message visibility details
     */
    @NonNull
    UpdateMessageResponse updateMessage(@NonNull String queueOcid,
                                        @NonNull String messageReceipt,
                                        int extensionSeconds);

    /**
     * Extend the visibility timeout of one or more messages.  If processing a message takes longer
     * than expected, consumers can extend the visibility timeout of a message. Extending the
     * timeout prevents the message from being returned to the queue and being delivered to another
     * consumer.
     *
     * @param queueOcid        the OCID of the queue
     * @param messageReceipts  opaque tokens uniquely representing the messages
     * @param extensionSeconds the new visibility of the message relative to the current time. If
     *                         the message is not explicitly deleted, it will become visible to
     *                         other consumers once this timeout is exhausted
     * @return updated message visibility details
     */
    @NonNull
    UpdateMessagesResponse updateMessages(@NonNull String queueOcid,
                                          @NonNull List<String> messageReceipts,
                                          int extensionSeconds);

    /**
     * Delete a message from the specified queue.
     *
     * @param queueOcid      the OCID of the queue
     * @param messageReceipt an opaque token uniquely representing the message
     * @return the delete response
     */
    @NonNull
    DeleteMessageResponse deleteMessage(@NonNull String queueOcid,
                                        @NonNull String messageReceipt);

    /**
     * Delete one or more messages from the specified queue.
     *
     * @param queueOcid       the OCID of the queue
     * @param messageReceipts opaque tokens uniquely representing the messages
     * @return the delete response
     */
    @NonNull
    DeleteMessagesResponse deleteMessages(@NonNull String queueOcid,
                                          @NonNull List<String> messageReceipts);

    /**
     * Put a message on the specified queue.
     *
     * @param queueOcid the OCID of the queue
     * @param channel   optional channel id to partition messages in a queue
     * @param message   the message to put on the queue. It can be in any format, including XML,
     *                  JSON, CSV, a Base64-encoded binary message, and even compressed formats
     *                  such as gzip
     * @param metadata  optional metadata to store with the message
     * @return the response
     */
    @NonNull
    PutMessagesResponse putMessage(@NonNull String queueOcid,
                                   @Nullable String channel,
                                   @NonNull String message,
                                   @Nullable Map<String, String> metadata);

    /**
     * Put one or more messages on the specified queue.
     *
     * @param queueOcid the OCID of the queue
     * @param channel   optional channel id to partition messages in a queue
     * @param messages  the messages to put on the queue. Each message string can be in any format,
     *                  including XML, JSON, CSV, a Base64-encoded binary message, and even
     *                  compressed formats such as gzip
     * @param metadata  optional metadata to store with the messages
     * @return the response
     */
    @NonNull
    PutMessagesResponse putMessages(@NonNull String queueOcid,
                                    @Nullable String channel,
                                    @NonNull List<String> messages,
                                    @Nullable List<Map<String, String>> metadata);

    /**
     * Get a message from the queue if one is immediately available, leveraging short polling
     * (won't block).
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to just get a message from the specified channel
     * @return the response
     */
    @NonNull
    GetMessagesResponse getMessage(@NonNull String queueOcid,
                                   @Nullable String channelFilter);

    /**
     * Get up to 20 messages from the queue that are immediately available, leveraging short polling
     * (won't block).
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to just get messages from the specified channel
     * @return the response
     */
    @NonNull
    GetMessagesResponse getMessages(@NonNull String queueOcid,
                                    @Nullable String channelFilter);

    /**
     * Get up to the specified number of messages from the queue, leveraging short polling
     * (won't block).
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to just get messages from the specified channel
     * @param limit         maximum number of messages to retrieve
     * @return the response
     */
    @NonNull
    GetMessagesResponse getMessages(@NonNull String queueOcid,
                                    @Nullable String channelFilter,
                                    @Max(20) int limit);

    /**
     * Get up to the specified number of messages from the queue, leveraging short polling (won't
     * block), and set visibility (lock) seconds to the specified value on each message.
     *
     * @param queueOcid           the OCID of the queue
     * @param channelFilter       optional filter to just get messages from the specified channel
     * @param limit               maximum number of messages to retrieve
     * @param visibilityInSeconds visibility of the message relative to the current time
     *                            (overriding config set on queue). If the message is not explicitly
     *                            deleted, it will become visible to other consumers once this
     *                            timeout is exhausted
     * @return the response
     */
    @NonNull
    GetMessagesResponse getMessages(@NonNull String queueOcid,
                                    @Nullable String channelFilter,
                                    @Max(20) int limit,
                                    int visibilityInSeconds);

    /**
     * Leveraging long polling, wait up to specified timeout seconds for a message to appear on the
     * queue, immediately returning the message if one becomes available.
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to just get a message from the specified channel
     * @param timeout       wait timeout in seconds
     * @return the response
     */
    @NonNull
    GetMessagesResponse waitMessage(@NonNull String queueOcid,
                                    @Nullable String channelFilter,
                                    @Min(5) @Max(30) int timeout);

    /**
     * Leveraging long polling, get up to 20 messages from the queue, waiting for up to the
     * specified timeout seconds for a message to appear. The service returns when any messages
     * become available, not necessarily the full 20.
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to just get messages from the specified channel
     * @param timeout       wait timeout in seconds
     * @return the response
     */
    @NonNull
    GetMessagesResponse waitMessages(@NonNull String queueOcid,
                                     @Nullable String channelFilter,
                                     @Min(5) @Max(30) int timeout);

    /**
     * Leveraging long polling, get up to the specified number of messages from the queue, waiting
     * for up to the specified timeout seconds for a message to appear. The service returns when any
     * messages become available, not necessarily the full amount requested.
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to just get messages from the specified channel
     * @param timeout       wait timeout in seconds
     * @param limit         maximum number of messages to retrieve
     * @return the response
     */
    @NonNull
    GetMessagesResponse waitMessages(@NonNull String queueOcid,
                                     @Nullable String channelFilter,
                                     @Min(5) @Max(30) int timeout,
                                     @Max(20) int limit);

    /**
     * Leveraging long polling, get up to the specified number of messages from the queue, waiting
     * for up to the specified timeout seconds for a message to appear, setting visibility (lock)
     * seconds to the specified value on each message. The service returns when any messages become
     * available, not necessarily the full amount requested.
     *
     * @param queueOcid           the OCID of the queue
     * @param channelFilter       optional filter to just get messages from the specified channel
     * @param timeout             wait timeout in seconds
     * @param limit               maximum number of messages to retrieve
     * @param visibilityInSeconds visibility of the message relative to the current time (overriding
     *                            config set on queue). If the message is not explicitly deleted, it
     *                            will become visible to other consumers once this timeout is exhausted
     * @return the response
     */
    @NonNull
    GetMessagesResponse waitMessages(@NonNull String queueOcid,
                                     @Nullable String channelFilter,
                                     @Min(5) @Max(30) int timeout,
                                     @Max(20) int limit,
                                     int visibilityInSeconds);

    /**
     * List channels active in the specified queue.
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to return only the specified channel
     * @return the channels
     */
    @NonNull
    ListChannelsResponse listChannels(@NonNull String queueOcid,
                                      @Nullable String channelFilter);

    /**
     * List channels active in the specified queue, starting from the specified pagination value.
     * Because channels are ephemeral, any list of channels retrieved is approximate and refreshed
     * once per second from past statistics information.
     *
     * @param queueOcid     the OCID of the queue
     * @param channelFilter optional filter to return only the specified channel
     * @param page          the pagination value
     * @return the channels
     */
    @NonNull
    ListChannelsResponse listChannels(@NonNull String queueOcid,
                                      @Nullable String channelFilter,
                                      @Nullable String page);

    /**
     * Get statistics for the Queue its dead letter queue, including the number of visible and
     * in-flight messages and the size of the queue in bytes. Visible messages are messages
     * currently in a queue that are available for consumption. In-flight messages are messages
     * delivered to a consumer but not yet deleted. In-flight messages are unavailable for
     * redelivery until their visibility timeout has passed.
     *
     * @param queueOcid the OCID of the queue
     * @return the stats
     */
    @NonNull
    GetStatsResponse getStats(@NonNull String queueOcid);
}
