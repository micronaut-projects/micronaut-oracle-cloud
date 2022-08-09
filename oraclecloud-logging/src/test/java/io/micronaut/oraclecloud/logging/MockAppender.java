package io.micronaut.oraclecloud.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class MockAppender extends AppenderBase<ILoggingEvent> {

    private static final List<ILoggingEvent> events = new ArrayList<>();

    static List<ILoggingEvent> getEvents() {
        return events;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        events.add(eventObject);
    }
}
