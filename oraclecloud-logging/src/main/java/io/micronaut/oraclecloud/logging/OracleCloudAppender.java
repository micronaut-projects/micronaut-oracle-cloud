/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.oraclecloud.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.QueueFactory;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.util.Duration;
import com.oracle.bmc.loggingingestion.model.LogEntry;
import com.oracle.bmc.loggingingestion.model.LogEntryBatch;
import com.oracle.bmc.loggingingestion.model.PutLogsDetails;
import com.oracle.bmc.loggingingestion.requests.PutLogsRequest;
import io.micronaut.core.annotation.Internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Oracle Cloud log appender for logback.
 *
 * @author Nemanja Mikic
 * @since 2.2.0
 */
@Internal
public final class OracleCloudAppender extends AppenderBase<ILoggingEvent> implements AppenderAttachable<ILoggingEvent> {

    private static final int DEFAULT_QUEUE_SIZE = 128;
    private static final int DEFAULT_EVENT_DELAY_TIMEOUT = 100;
    private static final int DEFAULT_MAX_BATCH_SIZE = 128;
    private static final String SPEC_VERSION = "1.0";
    private static final long DEFAULT_PUBLISH_PERIOD = 100;
    private final QueueFactory queueFactory = new QueueFactory();
    private final Duration eventDelayLimit = new Duration(DEFAULT_EVENT_DELAY_TIMEOUT);
    private final List<String> blackListLoggerName = new ArrayList<>();
    private Encoder<ILoggingEvent> encoder;
    private Future<?> task;
    private BlockingDeque<ILoggingEvent> deque;
    private String logId;
    private String source;
    private String subject;
    private String type;
    private int queueSize = DEFAULT_QUEUE_SIZE;
    private long publishPeriod = DEFAULT_PUBLISH_PERIOD;
    private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
    private Appender<ILoggingEvent> emergencyAppender;
    private boolean configuredSuccessfully = false;

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public long getPublishPeriod() {
        return publishPeriod;
    }

    public void setPublishPeriod(long publishPeriod) {
        this.publishPeriod = publishPeriod;
    }

    public void addBlackListLoggerName(String test) {
        this.blackListLoggerName.add(test);
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void start() {
        if (isStarted()) {
            return;
        }

        if (queueSize == 0) {
            addWarn("Queue size of zero is deprecated, use a size of one to indicate synchronous processing");
        }

        if (queueSize < 0) {
            addError("Queue size must be greater than zero");
            return;
        }

        if (publishPeriod <= 0) {
            addError("Publish period must be greater than zero");
            return;
        }

        if (encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return;
        }

        if (maxBatchSize <= 0) {
            addError("Max Batch size must be greater than zero");
            return;
        }

        if (logId == null) {
            addError("LogId not specified");
            return;
        }

        if (emergencyAppender != null && !emergencyAppender.isStarted()) {
            emergencyAppender.start();
        }

        deque = queueFactory.newLinkedBlockingDeque(queueSize);

        task = getContext().getScheduledExecutorService().scheduleAtFixedRate(() -> {
            try {
                dispatchEvents();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, publishPeriod, TimeUnit.MILLISECONDS);
        super.start();

    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        task.cancel(true);
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null || !isStarted() || blackListLoggerName.contains(eventObject.getLoggerName())) {
            return;
        }

        try {
            final boolean inserted = deque.offer(eventObject, eventDelayLimit.getMilliseconds(), TimeUnit.MILLISECONDS);
            if (!inserted) {
                addInfo("Dropping event due to timeout limit of [" + eventDelayLimit + "] being exceeded");
                if (emergencyAppender != null) {
                    emergencyAppender.doAppend(eventObject);
                }
            }
        } catch (InterruptedException e) {
            addError("Interrupted while appending event to SocketAppender", e);
            Thread.currentThread().interrupt();
        }
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    private boolean tryToConfigure() {

        if (!OracleCloudLoggingClient.isReady()) {
            return false;
        }

        String host = OracleCloudLoggingClient.getHost();
        String appName = OracleCloudLoggingClient.getAppName();

        if (type == null) {
            type = String.format("%s.%s", host, appName);
        }

        if (source == null) {
            source = host;
        }

        if (subject == null) {
            subject = appName;
        }

        configuredSuccessfully = true;

        return true;
    }

    private void dispatchEvents() throws InterruptedException {
        if (!configuredSuccessfully && !tryToConfigure()) {
            return;
        }

        List<LogEntry> logEntries = new ArrayList<>(maxBatchSize);
        List<ILoggingEvent> iLoggingEvents = new ArrayList<>(maxBatchSize);

        while (!deque.isEmpty() && logEntries.size() < maxBatchSize) {
            ILoggingEvent event = deque.takeFirst();
            final LogEntry inputLogEvent = LogEntry.builder().id(UUID.randomUUID().toString())
                    .data(new String(encoder.encode(event), StandardCharsets.UTF_8)).build();

            iLoggingEvents.add(event);
            logEntries.add(inputLogEvent);
        }
        if (!logEntries.isEmpty() && !sendLogsToOracleCloud(logEntries) && emergencyAppender != null) {
            iLoggingEvents.forEach(emergencyAppender::doAppend);
        }
    }

    private boolean sendLogsToOracleCloud(List<LogEntry> logEntries) {
        PutLogsDetails putLogsDetails = PutLogsDetails.builder()
                .logEntryBatches(Collections.singletonList(LogEntryBatch.builder()
                        .source(source)
                        .subject(subject)
                        .type(type)
                        .defaultlogentrytime(new Date())
                        .entries(logEntries)
                        .build())
                )
                .specversion(SPEC_VERSION)
                .build();
        PutLogsRequest putLogsRequest = PutLogsRequest.builder()
                .putLogsDetails(putLogsDetails)
                .logId(logId)
                .build();
        try {
            if (!OracleCloudLoggingClient.putLogs(putLogsRequest)) {
                addError("Sending log request failed");
            } else {
                return true;
            }
        } catch (Exception e) {
            addError("Sending log request failed", e);
        }
        return false;
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        if (emergencyAppender == null) {
            emergencyAppender = newAppender;
        } else {
            addWarn("One and only one appender may be attached to " + getClass().getSimpleName());
            addWarn("Ignoring additional appender named [" + newAppender.getName() + "]");
        }
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        throw new UnsupportedOperationException("Don't know how to create iterator");
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        if (emergencyAppender != null && name != null && name.equals(emergencyAppender.getName())) {
            return emergencyAppender;
        } else {
            return null;
        }
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return (emergencyAppender == appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        if (emergencyAppender != null) {
            emergencyAppender.stop();
            emergencyAppender = null;
        }
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        if (emergencyAppender == appender) {
            emergencyAppender = null;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean detachAppender(String name) {
        if (emergencyAppender != null && emergencyAppender.getName().equals(name)) {
            emergencyAppender = null;
            return true;
        } else {
            return false;
        }
    }
}
