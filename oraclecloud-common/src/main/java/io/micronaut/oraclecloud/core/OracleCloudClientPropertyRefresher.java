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
package io.micronaut.oraclecloud.core;

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.context.scope.refresh.RefreshEventListener;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Set;

/**
 * OracleCloudClientPropertyRefresher helps with refreshing the oracle cloud clients.
 */
@Singleton
public final class OracleCloudClientPropertyRefresher implements RefreshEventListener {

    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    public OracleCloudClientPropertyRefresher(ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public @NonNull Set<String> getObservedConfigurationPrefixes() {
        return Set.of(ServiceOracleCloudClientConfigurationProperties.PREFIX, OracleCloudClientConfigurationProperties.PREFIX);
    }

    @Override
    public void onApplicationEvent(RefreshEvent event) {
        eventPublisher.publishEvent(new RefreshEvent(Map.of("micronaut.http.client", "*")));
    }
}
