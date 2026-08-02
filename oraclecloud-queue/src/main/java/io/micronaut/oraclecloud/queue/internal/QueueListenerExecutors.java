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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.messaging.exceptions.MessagingSystemException;
import io.micronaut.oraclecloud.queue.GenericQueueListener;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration.QueueConfig;
import io.micronaut.oraclecloud.queue.annotation.QueueListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Helper class to find {@link GenericQueueListener} beans and associated data from
 * {@link QueueListener} annotations, and configure executors for each listener.
 */
class QueueListenerExecutors {

    private static final Logger LOG = LoggerFactory.getLogger(QueueListenerExecutors.class);

    private static final Pattern CONCURRENCY_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
    private static final long DEFAULT_KEEP_ALIVE_TIME = 500;

    private final Map<String, GenericQueueListener> listeners = new HashMap<>();
    private final Map<String, QueueListenerMetadata> listenerMetadata = new HashMap<>();
    private final Map<String, QueueConfig> listenerConfigs = new HashMap<>();
    private final Map<String, ExecutorService> listenerExecutors = new HashMap<>();

    QueueListenerExecutors(Collection<QueueConfig> queueConfigs,
                           ApplicationContext ctx) {

        // Find and if necessary, initialize Singleton beans for the given bean type, returning all
        // the active registrations. This method can return multiple registrations for a given
        // singleton bean instance since each bean may have multiple qualifiers.
        Collection<BeanRegistration<GenericQueueListener>> registrations =
            ctx.getBeanRegistrations(GenericQueueListener.class);

        List<String> fullQueueListenerTracking = new ArrayList<>();
        List<String> channelSpecificListenerTracking = new ArrayList<>();

        Map<String, QueueConfig> configsByName = queueConfigs.stream()
            .collect(Collectors.toMap(QueueConfig::getName, Function.identity()));

        for (BeanRegistration<GenericQueueListener> registration : registrations) {
            GenericQueueListener listener = Optional.ofNullable(registration.getBean()).orElseThrow(() ->
                new MessagingSystemException("GenericQueueListener bean instance associated with registration is null"));
            BeanDefinition<GenericQueueListener> beanDefinition = Optional.ofNullable(registration.getBeanDefinition()).orElseThrow(() ->
                new MessagingSystemException("GenericQueueListener bean definition associated with registration is null"));
            AnnotationValue<QueueListener> value = Optional.ofNullable(beanDefinition.getAnnotation(QueueListener.class)).orElseThrow(() ->
                new MessagingSystemException("GenericQueueListener bean definition is missing QueueListener annotation"));

            String name = value.stringValue("name")
                .orElse(value.stringValue("value").orElse(null));
            if (StringUtils.isEmpty(name)) {
                throw new MessagingSystemException("@QueueListener name is required");
            }
            LOG.debug("Discovered GenericQueueListener bean registration with configuration name {}", name);

            QueueConfig queueConfig = configsByName.get(name);
            if (queueConfig == null) {
                LOG.debug("Ignoring GenericQueueListener bean registration having configuration name {} - " +
                    "listener-enabled:true configuration is not present", name);
                continue;
            }

            QueueListenerMetadata metadata = getQueueListenerMetadata(value);

            String channelTaggedKey = getChannelTaggedKey(name, metadata);
            if (listeners.containsKey(channelTaggedKey)) {
                // see above - re multiple registrations for a given singleton bean instance potentially returned
                GenericQueueListener existing = listeners.get(channelTaggedKey);
                if (!existing.equals(listener)) {
                    LOG.warn("Multiple GenericQueueListener bean registrations have the same name {} - " +
                            "only the first listener receive new message events and the following registration will be ignored {}",
                        name, beanDefinition.getDeclaringType().get().getName());
                }
            } else {
                if ((metadata.isChannelLinked() && fullQueueListenerTracking.contains(name)) ||
                    (!metadata.isChannelLinked() && channelSpecificListenerTracking.contains(name))) {
                    throw new MessagingSystemException("Full-Queue and Channel-Specific Queue " +
                        "Listeners cannot be operated at the same time - " +
                        "Inspect queue listeners linked to: " + name);
                }

                if (metadata.isChannelLinked()) {
                    channelSpecificListenerTracking.add(name);
                } else {
                    fullQueueListenerTracking.add(name);
                }

                listeners.put(channelTaggedKey, listener);
                listenerMetadata.put(channelTaggedKey, metadata);
                listenerConfigs.put(channelTaggedKey, queueConfig);
                listenerExecutors.put(channelTaggedKey, getExecutorService(ctx, queueConfig));
            }
        }
    }

    Map<String, GenericQueueListener> getListeners() {
        return Collections.unmodifiableMap(listeners);
    }

    QueueListenerMetadata getListenerMetadata(String channelTaggedKey) {
        return listenerMetadata.get(channelTaggedKey);
    }

    QueueConfig getListenerConfig(String channelTaggedKey) {
        return listenerConfigs.get(channelTaggedKey);
    }

    ExecutorService getListenerExecutor(String channelTaggedKey) {
        return listenerExecutors.get(channelTaggedKey);
    }

    private static QueueListenerMetadata getQueueListenerMetadata(AnnotationValue<QueueListener> value) {
        return new QueueListenerMetadata(
            value.getRequiredValue("autoDelete", Boolean.class),
            value.getRequiredValue("autoExtendLease", Boolean.class),
            value.getRequiredValue("autoExtendLeaseSeconds", Integer.class),
            value.stringValue("channel")
                .filter(it -> !it.isEmpty())
                .orElse(null),
            value.getRequiredValue("proceedIfExpired", Boolean.class),
            value.getRequiredValue("proceedIfVisible", Boolean.class));
    }

    private String getChannelTaggedKey(String queueName, QueueListenerMetadata metadata) {
        return String.format("%s#[%s]", queueName, metadata.isChannelLinked() ? metadata.channel() : "");
    }

    private static ExecutorService getExecutorService(ApplicationContext ctx,
                                                      QueueConfig queue) {

        String executorName = queue.getListener().getExecutor();
        if (StringUtils.isNotEmpty(executorName)) {
            return ctx.findBean(ExecutorService.class, Qualifiers.byName(executorName)).orElseThrow(() ->
                new MessagingSystemException("No ExecutorService bean found with name " + executorName));
        }

        String concurrency = queue.getListener().getConcurrency();
        if (StringUtils.isEmpty(concurrency)) {
            throw new MessagingSystemException("Queue definition with name " + queue.getName() +
                " is missing listener executor/concurrency details");
        }

        Matcher matcher = CONCURRENCY_PATTERN.matcher(concurrency);
        if (!matcher.find() || matcher.groupCount() != 2) {
            throw new MessagingSystemException("Concurrency must be of the form int-int (e.g. '1-10'). " +
                "Concurrency provided was " + concurrency);
        }

        int numThreads = Integer.parseInt(matcher.group(1));
        if (numThreads < 0) {
            throw new MessagingSystemException("Concurrency threads (" + numThreads + ") cannot be negative");
        }

        int maxThreads = Integer.parseInt(matcher.group(2));
        if (maxThreads < 1) {
            throw new MessagingSystemException("Concurrency max threads (" + maxThreads + ") must be at least 0");
        }

        if (maxThreads < numThreads) {
            throw new MessagingSystemException("Concurrency threads (" + numThreads + ") " +
                "cannot be more than max threads (" + maxThreads + ")");
        }

        return new ThreadPoolExecutor(numThreads, maxThreads, DEFAULT_KEEP_ALIVE_TIME, MILLISECONDS,
            new LinkedBlockingQueue<>(numThreads), Executors.defaultThreadFactory());
    }
}
