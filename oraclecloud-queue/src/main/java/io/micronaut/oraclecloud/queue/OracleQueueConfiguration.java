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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Oracle Cloud Queue.
 */
@ConfigurationProperties(OracleQueueConfiguration.PREFIX)
public class OracleQueueConfiguration {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".queue";

    private String compartmentOcid;
    private List<QueueConfig> queues = new ArrayList<>();
    @Min(1)
    private int listenerInitialDelaySeconds = 15;
    @Min(1)
    private int listenerActivationFrequencySeconds = 15;
    private String namespace;
    @Min(0)
    private int adminMaxWaitTries = 20;
    @Min(0)
    private int adminWaitSleepSeconds = 15;

    /**
     * The default compartment OCID to use when not specified.
     *
     * @return the OCID
     */
    public String getCompartmentOcid() {
        return compartmentOcid;
    }

    /**
     * The default compartment OCID to use when not specified.
     *
     * @param compartmentOcid the OCID
     */
    public void setCompartmentOcid(String compartmentOcid) {
        this.compartmentOcid = compartmentOcid;
    }

    /**
     * The queue configurations.
     *
     * @return the configs
     */
    public List<QueueConfig> getQueues() {
        return queues;
    }

    /**
     * The queue configurations.
     *
     * @param queues the configs
     */
    public void setQueues(List<QueueConfig> queues) {
        this.queues = queues;
    }

    /**
     * The initial delay in seconds before the listener orchestrator starts to check for new messages.
     *
     * @return the frequency in seconds
     */
    public int getListenerInitialDelaySeconds() {
        return listenerInitialDelaySeconds;
    }

    /**
     * The initial delay in seconds before the listener orchestrator starts to check for new messages.
     *
     * @param listenerInitialDelaySeconds the frequency in seconds
     */
    public void setListenerInitialDelaySeconds(int listenerInitialDelaySeconds) {
        this.listenerInitialDelaySeconds = listenerInitialDelaySeconds;
    }

    /**
     * The frequency in seconds at which the listener orchestrator will fire to check for new messages.
     *
     * @return the frequency in seconds
     */
    public int getListenerActivationFrequencySeconds() {
        return listenerActivationFrequencySeconds;
    }

    /**
     * The frequency in seconds at which the listener orchestrator will fire to check for new messages.
     *
     * @param listenerActivationFrequencySeconds the frequency in seconds
     */
    public void setListenerActivationFrequencySeconds(int listenerActivationFrequencySeconds) {
        this.listenerActivationFrequencySeconds = listenerActivationFrequencySeconds;
    }

    /**
     * Optional namespace which will be used as a channel prefix if auto channeling is active.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Optional namespace which will be used as a channel prefix if auto channeling is active.
     *
     * @param namespace the namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * The maximum number of times to wait for an admin action, e.g., creating a queue, to finish.
     *
     * @return the maximum number of tries
     */
    public int getAdminMaxWaitTries() {
        return adminMaxWaitTries;
    }

    /**
     * The maximum number of times to wait for an admin action, e.g., creating a queue, to finish.
     * The default is 20 tries.
     *
     * @param adminMaxWaitTries the maximum number of tries
     */
    public void setAdminMaxWaitTries(int adminMaxWaitTries) {
        this.adminMaxWaitTries = adminMaxWaitTries;
    }

    /**
     * The number of seconds to wait between admin action retries. The default is 15 seconds.
     *
     * @return the number of seconds
     */
    public int getAdminWaitSleepSeconds() {
        return adminWaitSleepSeconds;
    }

    /**
     * The number of seconds to wait between admin action retries. The default is 15 seconds.
     *
     * @param adminWaitSleepSeconds the number of seconds
     */
    public void setAdminWaitSleepSeconds(int adminWaitSleepSeconds) {
        this.adminWaitSleepSeconds = adminWaitSleepSeconds;
    }

    /**
     * Configuration for a queue.
     */
    @EachProperty(value = "queues", list = true)
    public static class QueueConfig {

        private String name;
        private boolean enabled = false;
        private String ocid;
        private boolean autoChannel = false;
        private ListenerConfig listener = new ListenerConfig();

        /**
         * The queue name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * The queue name (must be unique).
         *
         * @param name the name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Whether this queue is enabled for use.
         *
         * @return true if enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Whether this queue is enabled for use.
         *
         * @param enabled true if enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * The OCID of the queue.
         *
         * @return the OCID
         */
        public String getOcid() {
            return ocid;
        }

        /**
         * The OCID of the queue.
         *
         * @param ocid the OCID
         */
        public void setOcid(String ocid) {
            this.ocid = ocid;
        }

        /**
         * Whether @Queue/@QueueListener will automatically inject the application workspace as a
         * channel prefix in put/get calls.
         * <p>
         * When directly calling interface methods that have a channel argument, the channel value
         * supplied will be appended to the namespace channel prefix if auto channeling is active,
         * otherwise only the specified channel argument is used.
         *
         * @return whether to prefix the channel with the namespace
         */
        public boolean isAutoChannel() {
            return autoChannel;
        }

        /**
         * Whether @Queue/@QueueListener will automatically inject the application workspace as a
         * channel prefix in put/get calls.
         *
         * @param autoChannel whether to prefix the channel with the namespace
         */
        public void setAutoChannel(boolean autoChannel) {
            this.autoChannel = autoChannel;
        }

        /**
         * The listener configuration.
         *
         * @return the config
         */
        public ListenerConfig getListener() {
            return listener;
        }

        /**
         * The listener configuration.
         *
         * @param listener the config
         */
        public void setListener(ListenerConfig listener) {
            this.listener = listener;
        }

        /**
         * Configuration for a queue listener.
         */
        @ConfigurationProperties("listener")
        public static class ListenerConfig {

            private boolean enabled = false;
            private String executor;
            @Pattern(regexp = "\\d+-\\d+")
            private String concurrency = "1-1";
            @Min(1)
            private int activationMultiple = 1;
            @Min(0)
            @Max(43200)
            private int messageVisibilityExclusivitySeconds = 0;
            private boolean longPolling = false;
            @Min(5)
            @Max(30)
            private int longPollingSeconds = 10;
            @Min(1)
            @Max(1000)
            private int maxMessagesPerActivation = 10;

            /**
             * Whether to poll in the background for new messages on this queue and trigger the
             * queue listener.
             *
             * @return true if enabled
             */
            public boolean isEnabled() {
                return enabled;
            }

            /**
             * Whether to poll in the background for new messages on this queue and trigger the
             * queue listener.
             *
             * @param enabled true if enabled
             */
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            /**
             * The name of a Micronaut thread pool executor that will handle background processing
             *
             * @return the executor qualifier name
             */
            public String getExecutor() {
                return executor;
            }

            /**
             * The name of a Micronaut thread pool executor that will handle background processing
             * of queue listener new message events.
             *
             * @param executor the executor qualifier name
             */
            public void setExecutor(String executor) {
                this.executor = executor;
            }

            /**
             * As an alternative to referencing a predefined thread pool executor above, you can
             * use this setting to create a thread pool to use in conjunction with the queue
             * listener. The value must be of the form x-y where x is the initial size of the thread
             * pool and y is the maximum size.
             * <p>
             * Note that when using channel-specific listeners, each channel has its own exclusive
             * thread pool when using this setting, whereas, when using the named executor, the
             * channel-specific listener(s) share the same executor.
             *
             * @return the concurrency
             */
            public String getConcurrency() {
                return concurrency;
            }

            /**
             * As an alternative to referencing a predefined thread pool executor above, you can
             * use this setting to create a thread pool to use in conjunction with the queue
             * listener. The value must be of the form x-y where x is the initial size of the thread
             * pool and y is the maximum size.
             * <p>
             * Note that when using channel-specific listeners, each channel has its own exclusive
             * thread pool when using this setting, whereas, when using the named executor, the
             * channel-specific listener(s) share the same executor.
             *
             * @param concurrency the concurrency
             */
            public void setConcurrency(String concurrency) {
                this.concurrency = concurrency;
            }

            /**
             * The listener orchestrator, which is scheduled to fire by default every 15 seconds (as
             * per the listenerActivationFrequencySeconds property above), is responsible for
             * triggering each queue's listener to check for new messages. An activation multiple of
             * 1 means that every time the orchestrator activates, the queue listener framework
             * activates. Whereas an activation multiple of 6 means that the queue listener only
             * activates every 1 in 6 orchestrator activations - such that, if the orchestrator
             * fires every 15 seconds, the queue would only be checked for messages every 90 seconds.
             * This multiplier can be used to prevent unnecessary frequent polling of queues that
             * seldom receive messages and where there is no urgency to process messages immediately.
             *
             * @return the activation multiple
             */
            public int getActivationMultiple() {
                return activationMultiple;
            }

            /**
             * The listener orchestrator, which is scheduled to fire by default every 15 seconds (as
             * per the listenerActivationFrequencySeconds property above), is responsible for
             * triggering each queue's listener to check for new messages. An activation multiple of
             * 1 means that every time the orchestrator activates, the queue listener framework
             * activates. Whereas an activation multiple of 6 means that the queue listener only
             * activates every 1 in 6 orchestrator activations - such that, if the orchestrator
             * fires every 15 seconds, the queue would only be checked for messages every 90 seconds.
             * This multiplier can be used to prevent unnecessary frequent polling of queues that
             * seldom receive messages and where there is no urgency to process messages immediately.
             *
             * @param activationMultiple the activation multiple
             */
            public void setActivationMultiple(int activationMultiple) {
                this.activationMultiple = activationMultiple;
            }

            /**
             * When the queue listener activates to check for messages, it can optionally provide a
             * visibilityInSeconds value to the queue service to set message exclusivity, overriding
             * the queue default visibilityInSeconds exclusivity value. The visibilityInSeconds
             * essentially acts as a message-locking mechanism to avoid multiple consumers
             * processing the same message concurrently. Once the visibility timeout period ends,
             * and if the message has not been explicitly deleted, another consumer could begin
             * processing the same message. It is important to note that the queue listener on
             * activation will attempt to get one or more messages and then enqueue these in the
             * appropriate thread pool executor (see above) for later processing when a thread is
             * free. Thus, it is possible that when a thread gets a chance to process the message
             * (per onMessageReceived(GetMessage) contract), the message visibility exclusivity
             * period may already have been exhausted, in which case the same message could appear
             * on another consumer. This is the reason the @QueueListener annotation has
             * proceedIfVisible and autoExtendLease settings.
             * <p>
             * Specifying a value of 0 will result in not passing of the visibilityInSeconds value
             * to the GetMessages method, in which case the queue service will return messages with
             * visibility exclusivity set to the queue level default (which is initially defined at
             * queue creation time).
             *
             * @return the visibility seconds
             */
            public int getMessageVisibilityExclusivitySeconds() {
                return messageVisibilityExclusivitySeconds;
            }

            /**
             * When the queue listener activates to check for messages, it can optionally provide a
             * visibilityInSeconds value to the queue service to set message exclusivity, overriding
             * the queue default visibilityInSeconds exclusivity value. The visibilityInSeconds
             * essentially acts as a message-locking mechanism to avoid multiple consumers
             * processing the same message concurrently. Once the visibility timeout period ends,
             * and if the message has not been explicitly deleted, another consumer could begin
             * processing the same message. It is important to note that the queue listener on
             * activation will attempt to get one or more messages and then enqueue these in the
             * appropriate thread pool executor (see above) for later processing when a thread is
             * free. Thus, it is possible that when a thread gets a chance to process the message
             * (per onMessageReceived(GetMessage) contract), the message visibility exclusivity
             * period may already have been exhausted, in which case the same message could appear
             * on another consumer. This is the reason the @QueueListener annotation has
             * proceedIfVisible and autoExtendLease settings.
             * <p>
             * Specifying a value of 0 will result in not passing of the visibilityInSeconds value
             * to the GetMessages method, in which case the queue service will return messages with
             * visibility exclusivity set to the queue level default (which is initially defined at
             * queue creation time).
             *
             * @param messageVisibilityExclusivitySeconds the visibility seconds
             */
            public void setMessageVisibilityExclusivitySeconds(int messageVisibilityExclusivitySeconds) {
                this.messageVisibilityExclusivitySeconds = messageVisibilityExclusivitySeconds;
            }

            /**
             * Short-polling means the OCI Queue Service returns whatever messages up to the
             * specified limit are immediately available for delivery and effectively closes the
             * HTTP request. Long-polling means the OCI Queue Service will keep the underlying HTTP
             * connection open in the event messages are not immediately available and potentially
             * return any messages that may subsequently appear in the ensuing long polling seconds.
             *
             * @return if true, use long polling, otherwise short polling
             */
            public boolean isLongPolling() {
                return longPolling;
            }

            /**
             * Short-polling means the OCI Queue Service returns whatever messages up to the
             * specified limit are immediately available for delivery and effectively closes the
             * HTTP request. Long-polling means the OCI Queue Service will keep the underlying HTTP
             * connection open in the event messages are not immediately available and potentially
             * return any messages that may subsequently appear in the ensuing long polling seconds.
             *
             * @param longPolling if true, use long polling, otherwise short polling
             */
            public void setLongPolling(boolean longPolling) {
                this.longPolling = longPolling;
            }

            /**
             * The long polling wait timeout in seconds, between 5 and 30.
             *
             * @return the timeout
             */
            public int getLongPollingSeconds() {
                return longPollingSeconds;
            }

            /**
             * The long polling wait timeout in seconds, between 5 and 30.
             *
             * @param longPollingSeconds the timeout
             */
            public void setLongPollingSeconds(int longPollingSeconds) {
                this.longPollingSeconds = longPollingSeconds;
            }

            /**
             * OCI Queue Services imposes an upper limit of 20 messages received per incoming
             * request. If there are many messages to process from the queue that can be handled by
             * the queue listener rapidly, multiple requests can be made serially to the queue to
             * retrieve the messages. These are then passed to the listener's thread pool executor
             * as described above. This will eventually fire <pre>onMessageReceived()</pre>
             * invocations as threads become available.
             * <p>
             * This property can be configured to attempt to restrict or obtain the specified number
             * of messages in the activation round.
             * <p>
             * Note that in the case of a clustered Micronaut application, the queue throughput best
             * case scenario would need to factor in the number of nodes in the cluster, multiplied
             * by the maximum messages per activation, multiplied by the listener activation rate.
             *
             * @return the max messages per activation
             */
            public int getMaxMessagesPerActivation() {
                return maxMessagesPerActivation;
            }

            /**
             * OCI Queue Services imposes an upper limit of 20 messages received per incoming
             * request. If there are many messages to process from the queue that can be handled by
             * the queue listener rapidly, multiple requests can be made serially to the queue to
             * retrieve the messages. These are then passed to the listener's thread pool executor
             * as described above. This will eventually fire <pre>onMessageReceived()</pre>
             * invocations as threads become available.
             * <p>
             * This property can be configured to attempt to restrict or obtain the specified number
             * of messages in the activation round.
             * <p>
             * Note that in the case of a clustered Micronaut application, the queue throughput best
             * case scenario would need to factor in the number of nodes in the cluster, multiplied
             * by the maximum messages per activation, multiplied by the listener activation rate.
             *
             * @param maxMessagesPerActivation the max messages per activation
             */
            public void setMaxMessagesPerActivation(int maxMessagesPerActivation) {
                this.maxMessagesPerActivation = maxMessagesPerActivation;
            }
        }
    }
}
