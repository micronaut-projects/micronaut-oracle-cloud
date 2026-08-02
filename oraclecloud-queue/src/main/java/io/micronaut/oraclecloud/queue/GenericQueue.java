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
package io.micronaut.oraclecloud.queue;

import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.PutMessage;
import com.oracle.bmc.queue.model.UpdatedMessage;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Interface implemented by beans for interacting with Oracle Cloud Queue, with methods for putting,
 * retrieving, deleting, and updating messages.
 */
public interface GenericQueue {

    /**
     * Put a message on the queue.
     *
     * @param message the message
     * @return the response object with id and expiration
     */
    @NonNull
    PutMessage putMessage(@NonNull String message);

    /**
     * Put a message on the queue.
     *
     * @param channel optional channel id to partition messages
     * @param message the message
     * @return the response object with id and expiration
     */
    @NonNull
    PutMessage putMessage(@Nullable String channel,
                          @NonNull String message);

    /**
     * Put a message on the queue.
     *
     * @param channel  optional channel id to partition messages
     * @param message  the message
     * @param metadata optional metadata to store with the message
     * @return the response object with id and expiration
     */
    @NonNull
    PutMessage putMessage(String channel,
                          @NonNull String message,
                          Map<String, String> metadata);

    /**
     * Put one or more messages on the queue.
     *
     * @param channel  optional channel id to partition messages
     * @param messages the messages
     * @param metadata optional metadata to store with the messages
     * @return the response objects with id and expiration
     */
    @NonNull
    List<PutMessage> putMessages(String channel,
                                 @NonNull List<String> messages,
                                 List<Map<String, String>> metadata);

    /**
     * Get a message from the queue if one is immediately available, leveraging short polling
     * (won't block).
     *
     * @return the message
     */
    @NonNull
    GetMessage getMessage();

    /**
     * Get a message from the queue if one is immediately available, leveraging short polling
     * (won't block).
     *
     * @param channelFilter optional filter to just get a message from the specified channel
     * @return the message
     */
    @NonNull
    GetMessage getMessage(String channelFilter);

    /**
     * Get a message from the queue if one is immediately available, leveraging short polling
     * (won't block).
     *
     * @param channelFilter       optional filter to just get a message from the specified channel
     * @param visibilityInSeconds visibility of the message relative to the current time
     *                            (overriding config set on queue). If the message is not explicitly
     *                            deleted, it will become visible to other consumers once this
     *                            timeout is exhausted
     * @return the message
     */
    @NonNull
    GetMessage getMessage(String channelFilter,
                          int visibilityInSeconds);

    /**
     * Get up to the specified number of messages from the queue, leveraging short polling (won't
     * block), and set visibility (lock) seconds to the specified value on each message.
     *
     * @param channelFilter optional filter to just get messages from the specified channel
     * @param limit         maximum number of messages to retrieve
     * @return the messages
     */
    @NonNull
    List<GetMessage> getMessages(String channelFilter,
                                 int limit);

    /**
     * Get up to the specified number of messages from the queue, leveraging short polling (won't
     * block), and set visibility (lock) seconds to the specified value on each message.
     *
     * @param channelFilter       optional filter to just get messages from the specified channel
     * @param limit               maximum number of messages to retrieve
     * @param visibilityInSeconds visibility of the messages relative to the current time
     *                            (overriding config set on queue). If the messages are not
     *                            explicitly deleted, they will become visible to other consumers
     *                            once this timeout is exhausted
     * @return the messages
     */
    @NonNull
    List<GetMessage> getMessages(String channelFilter,
                                 int limit,
                                 int visibilityInSeconds);

    /**
     * Leveraging long polling, get a message from the queue.
     *
     * @return the message
     */
    @NonNull
    GetMessage waitMessage();

    /**
     * Leveraging long polling, get a message from the queue.
     *
     * @param channelFilter optional filter to just get a message from the specified channel
     * @return the message
     */
    @NonNull
    GetMessage waitMessage(String channelFilter);

    /**
     * Leveraging long polling, get a message from the queue, waiting for up to the specified
     * timeout seconds for one to appear.
     *
     * @param channelFilter       optional filter to just get a message from the specified channel
     * @param visibilityInSeconds visibility of the message relative to the current time
     *                            (overriding config set on queue). If the message is not explicitly
     *                            deleted, it will become visible to other consumers once this
     *                            timeout is exhausted
     * @return the message
     */
    @NonNull
    GetMessage waitMessage(String channelFilter,
                           int visibilityInSeconds);

    /**
     * Leveraging long polling, get up to the specified number of messages from the queue. The
     * method returns when any messages become available, not necessarily the full amount requested.
     *
     * @param channelFilter optional filter to just get messages from the specified channel
     * @param limit         maximum number of messages to retrieve
     * @return the messages
     */
    @NonNull
    List<GetMessage> waitMessages(String channelFilter,
                                  int limit);

    /**
     * Leveraging long polling, get up to the specified number of messages from the queue, waiting
     * for up to the specified timeout seconds for a message to appear. The method returns when any
     * messages become available, not necessarily the full amount requested.
     *
     * @param channelFilter       optional filter to just get messages from the specified channel
     * @param limit               maximum number of messages to retrieve
     * @param visibilityInSeconds visibility of the messages relative to the current time
     *                            (overriding config set on queue). If the message is not explicitly
     *                            deleted, it will become visible to other consumers once this
     *                            timeout is exhausted
     * @return the messages
     */
    @NonNull
    List<GetMessage> waitMessages(String channelFilter,
                                  int limit,
                                  int visibilityInSeconds);

    /**
     * Deletes the message represented by the receipt from the queue.
     *
     * @param messageReceipt the receipt
     */
    void deleteMessage(@NonNull String messageReceipt);

    /**
     * Deletes the message represented by the message receipt from the queue.
     *
     * @param getMessage the message
     */
    void deleteMessage(@NonNull GetMessage getMessage);

    /**
     * Extend the visibility timeout of a message. If processing a message takes longer than
     * expected, consumers can extend the visibility timeout of a message. Extending the timeout
     * prevents the message from being returned to the queue and being delivered to another
     * consumer.
     *
     * @param messageReceipt      an opaque token uniquely representing the message
     * @param visibilityInSeconds the new visibility of the message relative to the current time.
     *                            If the message is not explicitly deleted, it will become visible to
     *                            other consumers once this timeout is exhausted
     * @return the updated message
     */
    @NonNull
    UpdatedMessage updateMessage(@NonNull String messageReceipt,
                                 int visibilityInSeconds);

    /**
     * Extend the visibility timeout of a message. If processing a message takes longer than
     * expected, consumers can extend the visibility timeout of a message. Extending the timeout
     * prevents the message from being returned to the queue and being delivered to another
     * consumer.
     *
     * @param getMessage          the message
     * @param visibilityInSeconds the new visibility of the message relative to the current time.
     *                            If the message is not explicitly deleted, it will become visible to
     *                            other consumers once this timeout is exhausted
     * @return the updated message
     */
    @NonNull
    UpdatedMessage updateMessage(@NonNull GetMessage getMessage,
                                 int visibilityInSeconds);
}
