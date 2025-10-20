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
package io.micronaut.oraclecloud.queue.internal;

import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.UpdatedMessage;
import io.micronaut.oraclecloud.queue.GenericQueueListener;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration.QueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import static io.micronaut.oraclecloud.queue.advice.AbstractQueueIntroductionAdvice.MAX_GET_MESSAGES_LIMIT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * The scheduled helper that looks for messages for queues with enabled listeners, i.e., beans
 * annotated with {@link GenericQueueListener}. The delay between scheduled invocations is defined
 * by the <pre>listenerActivationFrequencySeconds</pre> property in configuration.
 * <p>
 * For each invocation, the <pre>waitMessages</pre> method is called if long polling, or
 * <pre>getMessages</pre> otherwise, and for each message, <pre>onMessageReceived</pre> is called.
 * <p>
 * Additionally, if <pre>Queue.autoExtendLease</pre> is <pre>true</pre>, <pre>updateMessage</pre> is
 * called to extend, and if <pre>Queue.autoDelete</pre> is <pre>true</pre>, <pre>deleteMessage</pre>
 * is called.
 */
class QueueListenerOrchestrator implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(QueueListenerOrchestrator.class);

    private static final long DEFAULT_KEEP_ALIVE_TIME = 500;

    private final QueueListenerExecutors queueListenerExecutors;
    private final ThreadPoolExecutor executor;
    private final Map<String, Future<?>> queuePollingJobFutures = new HashMap<>();
    private final Map<String, AtomicLong> listenerActivations = new HashMap<>();

    QueueListenerOrchestrator(QueueListenerExecutors queueListenerExecutors) {
        this.queueListenerExecutors = queueListenerExecutors;

        for (String key : queueListenerExecutors.getListeners().keySet()) {
            listenerActivations.put(key, new AtomicLong(0));
        }

        int listenerCount = queueListenerExecutors.getListeners().size();
        executor = listenerCount == 0 ? null :
            new ThreadPoolExecutor(listenerCount, listenerCount, DEFAULT_KEEP_ALIVE_TIME, MILLISECONDS,
                new LinkedBlockingQueue<>(listenerCount), Executors.defaultThreadFactory());
    }

    @Override
    public void run() {
        queueListenerExecutors.getListeners().forEach((channelTaggedKey, listener) -> {
            QueueConfig queue = queueListenerExecutors.getListenerConfig(channelTaggedKey);

            if (0 != (listenerActivations.get(channelTaggedKey).getAndIncrement() % queue.getListener().getActivationMultiple())) {
                return; // skipping queue listener message poll this activation
            }

            Future<?> lastActivationFuture = queuePollingJobFutures.get(channelTaggedKey);
            if (lastActivationFuture != null && !lastActivationFuture.isDone()) {
                LOG.warn("Skipping polling of new messages for queue listener {} this activation" +
                    " - previous job still active", channelTaggedKey);
            }

            QueueListenerMetadata metadata = queueListenerExecutors.getListenerMetadata(channelTaggedKey);
            ExecutorService executorService = queueListenerExecutors.getListenerExecutor(channelTaggedKey);
            Future<?> future = executor.submit(new QueuePollingJob(listener, metadata, queue, executorService));
            queuePollingJobFutures.put(channelTaggedKey, future);
        });
    }

    private record QueuePollingJob(GenericQueueListener listener,
                                   QueueListenerMetadata metadata,
                                   QueueConfig queue,
                                   ExecutorService executorService) implements Runnable {

        @Override
        public void run() {
            // no need to do config.isAutoChannel() namespace channel setup here, as taken care of
            // in introduction advice in bean
            String channelFilter = metadata.isChannelLinked() ? metadata.channel() : null;
            int messagesRequired = queue.getListener().getMaxMessagesPerActivation();

            while (true) {

                int limit = Math.min(messagesRequired, MAX_GET_MESSAGES_LIMIT);
                int visibilitySeconds = queue.getListener().getMessageVisibilityExclusivitySeconds();

                List<GetMessage> messages;
                if (queue.getListener().isLongPolling()) {
                    if (visibilitySeconds > 0) {
                        messages = listener.waitMessages(channelFilter, limit, visibilitySeconds);
                    } else {
                        messages = listener.waitMessages(channelFilter, limit);
                    }
                } else {
                    if (visibilitySeconds > 0) {
                        messages = listener.getMessages(channelFilter, limit, visibilitySeconds);
                    } else {
                        messages = listener.getMessages(channelFilter, limit);
                    }
                }

                if (messages == null || messages.isEmpty()) {
                    break;
                }

                for (GetMessage message : messages) {
                    submitOnMessageReceivedJob(message);
                }

                messagesRequired -= messages.size();
                if (messagesRequired <= 0) {
                    break;
                }

                if (messages.size() != limit) {
                    // OCI does not guarantee that one will receive the full quota of messages
                    break;
                }
            }
        }

        private void submitOnMessageReceivedJob(GetMessage message) {
            if (message == null) {
                return;
            }

            executorService.submit(() -> {

                if (!metadata.proceedIfExpired() && listener.isMessageExpired(message)) {
                    LOG.debug("Skipping processing of message {} received on queue {}" +
                            " - message now appears expired",
                        message, queue.getName());
                    return;
                }

                if (!metadata.proceedIfVisible() && listener.isMessageVisibleToOthers(message)) {
                    LOG.debug("Skipping processing of message {} received on queue {}" +
                            " - message now appears to be visible to other consumers",
                        message, queue.getName());
                    return;
                }

                GetMessage messageRevised = message;
                if (metadata.autoExtendLease() && !listener.isMessageVisibleToOthers(message)) {
                    UpdatedMessage updatedMessage = listener.updateMessage(message, metadata.autoExtendLeaseSeconds());
                    if (updatedMessage != null && updatedMessage.getVisibleAfter() != null &&
                        !updatedMessage.getVisibleAfter().equals(message.getVisibleAfter())) {
                        messageRevised = message.toBuilder().visibleAfter(updatedMessage.getVisibleAfter()).build();
                    }
                }

                listener.onMessageReceived(messageRevised);

                if (metadata.autoDelete()) {
                    if (listener.isMessageVisibleToOthers(messageRevised)) {
                        LOG.warn("Skipping delete of message {} received on queue {}" +
                                " - message now appears to be visible to other consumers",
                            messageRevised, queue.getName());
                    } else {
                        listener.deleteMessage(messageRevised);
                    }
                }
            });
        }
    }
}
