/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.oraclecloud.monitoring.sdk;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.oraclecloud.httpclient.netty.OciNettyClientFilter;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

import static io.micronaut.oraclecloud.httpclient.netty.NettyClientProperties.CLASS_AND_METHOD_KEY_NAME;

/**
 * The SdkMetricsNettyClientFilter will emit oci sdk client metrics.
 *
 * @since 4.2.0
 * @author Nemanja Mikic
 */
@Singleton
@Requires(beans = MeterRegistry.class)
@Requires(property = MeterRegistryFactory.MICRONAUT_METRICS_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Requires(property = SdkMetricsNettyClientFilter.MICRONAUT_METRICS_OCI_SDK_CLIENT_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
public class SdkMetricsNettyClientFilter implements OciNettyClientFilter<Timer.Sample> {

    public static final String MICRONAUT_METRICS_OCI_SDK_CLIENT_ENABLED = "micronaut.metrics.oci.sdk.client.enabled";

    private static final String METHOD = "http_method";
    private static final String STATUS = "status";
    private static final String HOST = "host";
    private static final String EXCEPTION = "exception";
    private static final String CLASS_NAME = "class_and_method";

    private static final String METRICS_NAME = "oci.sdk.client";

    private final Provider<MeterRegistry> meterRegistryProvider;

    @Nullable
    private volatile MeterRegistry meterRegistry;

    public SdkMetricsNettyClientFilter(Provider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @EventListener
    final void onStartup(StartupEvent event) {
        // Registry creation can make OCI requests while initializing datasource binders.
        meterRegistry = meterRegistryProvider.get();
    }

    @Override
    public Timer.Sample beforeRequest(@NonNull HttpRequest request) {
        MeterRegistry registry = meterRegistry;
        return registry == null ? Timer.start() : Timer.start(registry);
    }

    @Override
    public HttpResponse afterResponse(@NonNull HttpRequest request, @Nullable HttpResponse response, @Nullable Throwable throwable, Timer.@NonNull Sample timerSample) {
        MeterRegistry registry = meterRegistry;
        if (registry == null) {
            return response;
        }

        List<Tag> tags = new ArrayList<>(4);

        tags.add(Tag.of(HOST, request.uri().getHost()));
        tags.add(Tag.of(METHOD, request.method().name()));
        tags.add(Tag.of(CLASS_NAME, (String) request.attribute(CLASS_AND_METHOD_KEY_NAME)));

        tags.add(exception(throwable));

        if (response != null) {
            tags.add(Tag.of(STATUS, String.valueOf(response.status())));
        }

        final Timer timer = Timer.builder(METRICS_NAME)
            .description("oci sdk client metrics")
            .tags(tags)
            .register(registry);
        timerSample.stop(timer);
        return response;
    }

    /**
     * Get a tag with the throwable.
     *
     * @param throwable a throwable exception
     * @return Tag of exception class name
     */
    private static Tag exception(Throwable throwable) {
        if (throwable == null) {
            return Tag.of(EXCEPTION, "none");
        }
        return Tag.of(EXCEPTION, throwable.getClass().getSimpleName());
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
