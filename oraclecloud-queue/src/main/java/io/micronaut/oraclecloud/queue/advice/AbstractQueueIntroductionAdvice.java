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
package io.micronaut.oraclecloud.queue.advice;

import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.PutMessage;
import com.oracle.bmc.queue.model.UpdatedMessage;
import com.oracle.bmc.queue.responses.DeleteMessageResponse;
import com.oracle.bmc.queue.responses.GetMessagesResponse;
import com.oracle.bmc.queue.responses.PutMessagesResponse;
import com.oracle.bmc.queue.responses.UpdateMessageResponse;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.StringUtils;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration.QueueConfig;
import io.micronaut.oraclecloud.queue.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.micronaut.http.HttpStatus.NO_CONTENT;
import static io.micronaut.http.HttpStatus.OK;

/**
 * Base class for introduction advice for queue listeners.
 *
 * @param <A> the annotation type
 */
public abstract class AbstractQueueIntroductionAdvice<A extends Annotation>
    implements MethodInterceptor<Object, Object> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * See <a href="https://docs.oracle.com/en-us/iaas/api/#/en/queue/20210201/GetMessage/GetMessages">the API documentation</a>.
     */
    public static final int MAX_VISIBILITY_SECONDS = 43200;

    /**
     * See <a href="https://docs.oracle.com/en-us/iaas/api/#/en/queue/20210201/GetMessage/GetMessages">the API documentation</a>.
     */
    public static final int MAX_GET_MESSAGES_LIMIT = 20;

    private final QueueService service;
    private final OracleQueueConfiguration config;
    private final Class<A> annotationClass;
    private final String annotationName;
    private final Map<String, QueueConfig> configsByName;

    protected AbstractQueueIntroductionAdvice(QueueService service,
                                              OracleQueueConfiguration config,
                                              Class<A> annotationClass) {
        this.service = service;
        this.config = config;
        this.annotationClass = annotationClass;
        annotationName = "@" + annotationClass.getSimpleName();
        configsByName = config.getQueues().stream()
            .filter(QueueConfig::isEnabled)
            .collect(Collectors.toMap(QueueConfig::getName, Function.identity()));

    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {

        Class<?> type = context.getDeclaringType();
        if (!isSupportedDeclaringType(type)) {
            log.warn("{} annotation present on an unsupported class {}",
                annotationName, type.getName());
            return null;
        }

        AnnotationValue<A> value = context.getAnnotation(annotationClass);
        String queueName = value.stringValue("name")
            .orElse(value.stringValue("value").orElse(null));

        if (StringUtils.isEmpty(queueName)) {
            log.warn("{} annotation name attribute must be provided to link to a queue from configuration",
                annotationName);
            return null;
        }

        QueueConfig queue = configsByName.get(queueName);
        if (queue == null) {
            log.warn("{} annotation name/value attribute {} must reference an enabled queue from configuration",
                annotationName, queueName);
            return null;
        }

        return handleMethod(context, context.getMethodName(), queue, queue.getOcid());
    }

    protected abstract boolean isSupportedDeclaringType(Class<?> type);

    private Object handleMethod(MethodInvocationContext<Object, Object> context,
                                String methodName,
                                QueueConfig queue,
                                String ocid) {
        return switch (methodName) {
            case "getMessage", "waitMessage" -> handleGetMessage(context, queue, ocid);
            case "getMessages", "waitMessages" -> handleGetMessages(context, queue, ocid);
            case "putMessage" -> handlePutMessage(context, queue, ocid);
            case "putMessages" -> handlePutMessages(context, queue, ocid);
            case "deleteMessage" -> {
                handleDeleteMessage(context, ocid);
                yield null;
            }
            case "updateMessage" -> handleUpdateMessage(context, ocid);
            default -> {
                log.warn("QueueListener method {} currently unhandled - no introduction logic present", methodName);
                yield null;
            }
        };
    }

    protected GetMessage handleGetMessage(MethodInvocationContext<Object, Object> context,
                                          QueueConfig queue,
                                          String ocid) {

        int visibilityInSeconds = visibilityInSeconds(context, -1);
        String channelFilter = channelFilter(context, queue);

        GetMessagesResponse response;
        if ("waitMessage".equals(context.getMethodName())) {
            response = visibilityInSeconds > -1
                ? service.waitMessages(ocid, channelFilter, queue.getListener().getLongPollingSeconds(), 1, visibilityInSeconds)
                : service.waitMessage(ocid, channelFilter, queue.getListener().getLongPollingSeconds());
        } else {
            response = visibilityInSeconds > -1
                ? service.getMessages(ocid, channelFilter, 1, visibilityInSeconds)
                : service.getMessage(ocid, channelFilter);
        }

        if (OK.getCode() != response.get__httpStatusCode__()) {
            log.warn("Queue service returned status code {} for the get message operation: {}",
                response.get__httpStatusCode__(), response);
            return null;
        }

        return response.getGetMessages().getMessages().stream().findFirst().orElse(null);
    }

    protected List<GetMessage> handleGetMessages(MethodInvocationContext<Object, Object> context,
                                                 QueueConfig queue,
                                                 String ocid) {

        int limit = getParameter(context, "limit", Integer.class);
        limit = limit <= 0 || limit > MAX_GET_MESSAGES_LIMIT ? MAX_GET_MESSAGES_LIMIT : limit;

        int visibilityInSeconds = visibilityInSeconds(context, -1);
        String channelFilter = channelFilter(context, queue);

        GetMessagesResponse response;
        if ("waitMessages".equals(context.getMethodName())) {
            response = visibilityInSeconds > -1
                ? service.waitMessages(ocid, channelFilter, queue.getListener().getLongPollingSeconds(), limit, visibilityInSeconds)
                : service.waitMessages(ocid, channelFilter, queue.getListener().getLongPollingSeconds(), limit);
        } else {
            response = visibilityInSeconds > -1
                ? service.getMessages(ocid, channelFilter, limit, visibilityInSeconds)
                : service.getMessages(ocid, channelFilter, limit);
        }

        if (OK.getCode() == response.get__httpStatusCode__()) {
            return response.getGetMessages().getMessages();
        }

        log.warn("Queue service returned status code {} for the get messages operation: {}",
            response.get__httpStatusCode__(), response);
        return null;
    }

    protected PutMessage handlePutMessage(MethodInvocationContext<Object, Object> context,
                                          QueueConfig queue,
                                          String ocid) {

        String channel = getChannel(context, queue);
        @SuppressWarnings("unchecked")
        Map<String, String> metadata = getParameter(context, "metadata", Map.class);
        String message = getParameter(context, "message", String.class);

        PutMessagesResponse response = service.putMessage(ocid, channel, message, metadata);
        if (OK.getCode() != response.get__httpStatusCode__()) {
            log.warn("Queue service returned status code {} for the put message operation: {}",
                response.get__httpStatusCode__(), response);
            return null;
        }

        return response.getPutMessages().getMessages().stream().findFirst().orElse(null);
    }

    protected List<PutMessage> handlePutMessages(MethodInvocationContext<Object, Object> context,
                                                 QueueConfig queue,
                                                 String ocid) {
        String channel = getChannel(context, queue);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> metadata = getParameter(context, "metadata", List.class);
        @SuppressWarnings("unchecked")
        List<String> messages = getParameter(context, "messages", List.class);

        PutMessagesResponse response = service.putMessages(ocid, channel, messages, metadata);
        if (OK.getCode() != response.get__httpStatusCode__()) {
            log.warn("Queue service returned status code {} for the put messages operation: {}",
                response.get__httpStatusCode__(), response);
            return null;
        }

        return response.getPutMessages().getMessages();
    }

    protected UpdatedMessage handleUpdateMessage(MethodInvocationContext<Object, Object> context,
                                                 String ocid) {

        UpdateMessageResponse response = service.updateMessage(
            ocid, messageReceipt(context), visibilityInSeconds(context, 0));
        if (OK.getCode() != response.get__httpStatusCode__()) {
            log.warn("Queue service returned status code {} for the update message operation: {}",
                response.get__httpStatusCode__(), response);
            return null;
        }

        return response.getUpdatedMessage();
    }

    protected void handleDeleteMessage(MethodInvocationContext<Object, Object> context,
                                       String ocid) {

        DeleteMessageResponse response = service.deleteMessage(ocid, messageReceipt(context));
        if (NO_CONTENT.getCode() != response.get__httpStatusCode__()) {
            log.warn("Queue service returned status code {} for the delete message operation: {}",
                response.get__httpStatusCode__(), response);
        }
    }

    private String getChannel(MethodInvocationContext<Object, Object> context,
                              QueueConfig queue) {
        String channel = queue.isAutoChannel() ? config.getNamespace() : null;
        String channelParam = getParameter(context, "channel", String.class);
        if (channelParam != null) {
            channel = channel == null ? channelParam : channel + '/' + channelParam;
        }
        return channel;
    }

    private String channelFilter(MethodInvocationContext<Object, Object> context,
                                 QueueConfig queue) {
        String channelFilter = queue.isAutoChannel() ? config.getNamespace() : null;
        String channelParam = getParameter(context, "channelFilter", String.class);
        if (channelParam != null) {
            channelFilter = channelFilter == null ? channelParam : channelFilter + '/' + channelParam;
        }
        return channelFilter;
    }

    private String messageReceipt(MethodInvocationContext<Object, Object> context) {

        if (context.getParameterValueMap().containsKey("messageReceipt")) {
            return (String) context.getParameterValueMap().get("messageReceipt");
        }

        if (context.getParameterValueMap().containsKey("getMessage")) {
            return ((GetMessage) context.getParameterValueMap().get("getMessage")).getReceipt();
        }

        return null;
    }

    private int visibilityInSeconds(MethodInvocationContext<Object, Object> context,
                                    int minValue) {
        int seconds = minValue;
        if (context.getParameterValueMap().containsKey("visibilityInSeconds")) {
            seconds = (int) context.getParameterValueMap().get("visibilityInSeconds");
            seconds = Math.max(minValue, Math.min(MAX_VISIBILITY_SECONDS, seconds));
        }
        return seconds;
    }

    private <T> T getParameter(MethodInvocationContext<Object, Object> context,
                               String name, Class<T> type) {
        return context.getParameterValueMap().containsKey(name)
            ? type.cast(context.getParameterValueMap().get(name)) : null;
    }
}
