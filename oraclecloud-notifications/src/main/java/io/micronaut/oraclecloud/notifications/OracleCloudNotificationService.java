/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.oraclecloud.notifications;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.ons.NotificationControlPlane;
import com.oracle.bmc.ons.NotificationDataPlane;
import com.oracle.bmc.ons.NotificationDataPlaneClient;
import com.oracle.bmc.ons.model.MessageDetails;
import com.oracle.bmc.ons.requests.GetTopicRequest;
import com.oracle.bmc.ons.requests.PublishMessageRequest;
import com.oracle.bmc.ons.responses.PublishMessageResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.ArgumentUtils;
import io.micronaut.core.util.StringUtils;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes messages to Oracle Cloud Notifications topics.
 *
 * @since 6.0.0
 */
@Singleton
@Requires(bean = NotificationControlPlane.class)
@Requires(bean = NotificationDataPlaneFactory.class)
@Requires(property = OracleCloudNotificationsConfiguration.ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
public class OracleCloudNotificationService {

    private final NotificationControlPlane notificationControlPlane;
    private final NotificationDataPlaneFactory notificationDataPlaneFactory;
    private final OracleCloudNotificationsConfiguration configuration;
    private final Map<String, String> topicEndpoints = new ConcurrentHashMap<>();
    private final Map<String, NotificationDataPlane> notificationDataPlanes = new ConcurrentHashMap<>();

    /**
     * @param notificationControlPlane The OCI Notifications control plane client.
     * @param notificationDataPlaneFactory The OCI Notifications data plane client factory.
     * @param configuration The Notifications configuration.
     */
    public OracleCloudNotificationService(
            NotificationControlPlane notificationControlPlane,
            NotificationDataPlaneFactory notificationDataPlaneFactory,
            OracleCloudNotificationsConfiguration configuration) {
        this.notificationControlPlane = notificationControlPlane;
        this.notificationDataPlaneFactory = notificationDataPlaneFactory;
        this.configuration = configuration;
    }

    /**
     * Publishes a message to the configured topic.
     *
     * @param title The message title.
     * @param body The message body.
     * @return The publish response.
     */
    public PublishMessageResponse publish(@NonNull String title, @NonNull String body) {
        return publish(configuredTopicId(), title, body);
    }

    /**
     * Publishes a message to a topic.
     *
     * @param topicId The topic OCID.
     * @param title The message title.
     * @param body The message body.
     * @return The publish response.
     */
    public PublishMessageResponse publish(@NonNull String topicId, @NonNull String title, @NonNull String body) {
        requireNotEmpty("topicId", topicId);
        requireNotEmpty("title", title);
        requireNotEmpty("body", body);
        return publish(PublishMessageRequest.builder()
            .topicId(topicId)
            .messageDetails(MessageDetails.builder()
                .title(title)
                .body(body)
                .build())
            .build());
    }

    /**
     * Publishes a pre-built OCI SDK request.
     *
     * @param request The publish request.
     * @return The publish response.
     */
    public PublishMessageResponse publish(@NonNull PublishMessageRequest request) {
        ArgumentUtils.requireNonNull("request", request);
        String topicId = request.getTopicId();
        requireNotEmpty("request.topicId", topicId);
        String topicEndpoint = topicEndpoints.computeIfAbsent(topicId, this::resolveTopicEndpoint);
        NotificationDataPlane notificationDataPlane = notificationDataPlanes.computeIfAbsent(topicEndpoint, notificationDataPlaneFactory::create);
        return notificationDataPlane.publishMessage(request);
    }

    @PreDestroy
    final void close() throws Exception {
        Exception failure = null;
        for (NotificationDataPlane notificationDataPlane : notificationDataPlanes.values()) {
            try {
                notificationDataPlane.close();
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private String configuredTopicId() {
        return configuration.getTopicId()
            .filter(StringUtils::isNotEmpty)
            .orElseThrow(() -> new ConfigurationException("Oracle Cloud Notifications requires configuration property [" + OracleCloudNotificationsConfiguration.PREFIX + ".topic-id]"));
    }

    private String resolveTopicEndpoint(String topicId) {
        String apiEndpoint = notificationControlPlane.getTopic(GetTopicRequest.builder()
                .topicId(topicId)
                .build())
            .getNotificationTopic()
            .getApiEndpoint();
        if (StringUtils.isEmpty(apiEndpoint)) {
            throw new ConfigurationException("Oracle Cloud Notifications topic [" + topicId + "] does not define an API endpoint");
        }
        return apiEndpoint;
    }

    private static void requireNotEmpty(String name, String value) {
        ArgumentUtils.requireNonNull(name, value);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Argument [" + name + "] cannot be empty");
        }
    }
}

@Internal
@Singleton
@Requires(bean = NotificationDataPlaneClient.Builder.class)
@Requires(bean = AbstractAuthenticationDetailsProvider.class)
final class DefaultNotificationDataPlaneFactory implements NotificationDataPlaneFactory {
    private final NotificationDataPlaneClient.Builder notificationDataPlaneBuilder;
    private final AbstractAuthenticationDetailsProvider authenticationDetailsProvider;

    DefaultNotificationDataPlaneFactory(
            NotificationDataPlaneClient.Builder notificationDataPlaneBuilder,
            AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
        this.notificationDataPlaneBuilder = notificationDataPlaneBuilder;
        this.authenticationDetailsProvider = authenticationDetailsProvider;
    }

    @Override
    public NotificationDataPlane create(String endpoint) {
        NotificationDataPlaneClient.Builder endpointBuilder = NotificationDataPlaneClient.builder();
        synchronized (notificationDataPlaneBuilder) {
            endpointBuilder.copyFrom(notificationDataPlaneBuilder);
        }
        return endpointBuilder
            .endpoint(endpoint)
            .build(authenticationDetailsProvider);
    }
}
