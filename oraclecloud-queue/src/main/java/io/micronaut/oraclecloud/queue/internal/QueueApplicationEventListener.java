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
import io.micronaut.context.event.StartupEvent;
import io.micronaut.messaging.exceptions.MessagingSystemException;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration.QueueConfig;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskScheduler;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static io.micronaut.scheduling.TaskExecutors.SCHEDULED;

/**
 * At startup, schedules  {@link QueueListenerOrchestrator} to look for messages on queues with
 * enabled listeners.
 */
@Singleton
public class QueueApplicationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(QueueApplicationEventListener.class);

    private final ApplicationContext ctx;
    private final OracleQueueConfiguration config;
    private final TaskScheduler scheduler;

    QueueApplicationEventListener(ApplicationContext ctx,
                                  OracleQueueConfiguration config,
                                  @Named(SCHEDULED) TaskScheduler scheduler) {
        this.ctx = ctx;
        this.config = config;
        this.scheduler = scheduler;
    }

    @EventListener
    public void onStartup(StartupEvent event) {

        List<QueueConfig> queueConfigs = config.getQueues();
        if (queueConfigs.isEmpty()) {
            LOG.debug("Not scheduling listeners, no queues are configured");
            return;
        }

        List<QueueConfig> enabledConfigs = queueConfigs.stream()
            .filter(QueueConfig::isEnabled)
            .toList();
        if (!enabledConfigs.stream().map(QueueConfig::getName).allMatch(new HashSet<>()::add)) {
            throw new MessagingSystemException("Enabled queue definitions in configuration must have unique names");
        }

        Collection<QueueConfig> listenerEnabledConfigs = enabledConfigs.stream()
            .filter(queue -> queue.getListener().isEnabled())
            .toList();
        if (listenerEnabledConfigs.isEmpty()) {
            LOG.debug("Not scheduling listeners, no queues have enabled listener configs");
            return;
        }

        LOG.debug("Scheduling listeners with initial delay {} and scheduled delay {}",
            config.getListenerInitialDelaySeconds(), config.getListenerActivationFrequencySeconds());
        scheduler.scheduleWithFixedDelay(
            Duration.ofSeconds(config.getListenerInitialDelaySeconds()),
            Duration.ofSeconds(config.getListenerActivationFrequencySeconds()),
            new QueueListenerOrchestrator(new QueueListenerExecutors(listenerEnabledConfigs, ctx)));
    }
}
