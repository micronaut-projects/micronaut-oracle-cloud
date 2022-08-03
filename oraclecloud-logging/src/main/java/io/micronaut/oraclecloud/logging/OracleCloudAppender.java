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
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.QueueFactory;
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
public final class OracleCloudAppender extends AppenderBase<ILoggingEvent> {

    private static final int DEFAULT_QUEUE_SIZE = 128;
    private static final int DEFAULT_EVENT_DELAY_TIMEOUT = 100;
    private static final String SPEC_VERSION = "1.0";
    private final QueueFactory queueFactory = new QueueFactory();
    private final Duration eventDelayLimit = new Duration(DEFAULT_EVENT_DELAY_TIMEOUT);
    private Encoder<ILoggingEvent> encoder;
    private Future<?> task;
    private BlockingDeque<ILoggingEvent> deque;
    private String logId;
    private String host;
    private String appName;
    private int queueSize = DEFAULT_QUEUE_SIZE;

    private final List<String> blackListLoggerName = new ArrayList<>();

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
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

        if (encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return;
        }

        if (logId == null) {
            addError("LogId not specified");
            return;
        }
        deque = queueFactory.newLinkedBlockingDeque(queueSize);

        task = getContext().getScheduledExecutorService().scheduleAtFixedRate(() -> {
            try {
                dispatchEvents();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            addInfo("shutting down");
        }, 0, 100, TimeUnit.MILLISECONDS);
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

        if (OracleCloudLoggingClient.getLogging() == null) {
            return false;
        }

        if (host == null) {
            host = OracleCloudLoggingClient.getHost();
        }

        if (appName == null) {
            appName = OracleCloudLoggingClient.getAppName();
        }

        return true;
    }

    private void dispatchEvents() throws InterruptedException {
        if (!tryToConfigure()) {
            return;
        }
        while (!deque.isEmpty()) {
            ILoggingEvent event = deque.takeFirst();
            PutLogsDetails putLogsDetails = PutLogsDetails.builder()
                    .logEntryBatches(Collections.singletonList(LogEntryBatch.builder()
                            .source(host)
                            .subject(event.getLoggerName())
                            .type(String.format("%s.%s.%s", appName,  host, event.getLevel()))
                            .defaultlogentrytime(new Date())
                            .entries(Collections.singletonList(LogEntry.builder()
                                    .id(UUID.randomUUID().toString())
                                    .data(new String(encoder.encode(event), StandardCharsets.UTF_8)).build()))
                            .build())
                    )
                    .specversion(SPEC_VERSION)
                    .build();
            PutLogsRequest putLogsRequest = PutLogsRequest.builder()
                    .putLogsDetails(putLogsDetails)
                    .logId(logId)
                    .build();
                if (!OracleCloudLoggingClient.putLogs(putLogsRequest)) {
                    addError("Sending log request failed");
                }

        }
    }

}
