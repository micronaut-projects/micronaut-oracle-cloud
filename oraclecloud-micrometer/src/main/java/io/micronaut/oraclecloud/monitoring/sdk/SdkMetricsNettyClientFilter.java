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
import io.micronaut.core.util.StringUtils;
import io.micronaut.oraclecloud.httpclient.netty.NettyClientFilter;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The SdkMetricsNettyClientFilter will emit oci sdk client metrics.
 */
@Singleton
@Requires(property = MeterRegistryFactory.MICRONAUT_METRICS_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Requires(property = SdkMetricsNettyClientFilter.MICRONAUT_METRICS_OCI_SDK_CLIENT_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
public class SdkMetricsNettyClientFilter implements NettyClientFilter {

    public static final String MICRONAUT_METRICS_OCI_SDK_CLIENT_ENABLED = "micronaut.metrics.oci.sdk.client.enabled";

    static final String UNKNOWN = "UNKNOWN";

    private static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");
    private static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");
    private static final Tag URI_UNAUTHORIZED = Tag.of("uri", "UNAUTHORIZED");
    private static final Tag URI_BAD_REQUEST = Tag.of("uri", "BAD_REQUEST");
    private static final String METHOD = "method";
    private static final String STATUS = "status";
    private static final String URI = "uri";
    private static final String EXCEPTION = "exception";
    private static final String SERVICE_ID = "serviceId";

    private static final String METRICS_TIMER = "oraclecloud.monitoring.sdk.timer";
    private static final String METRICS_NAME = "oci.sdk.client";

    private final Provider<MeterRegistry> meterRegistryProvider;

    private final SdkMetricsNettyClientFilterConfig sdkMetricsNettyClientFilterConfig;

    public SdkMetricsNettyClientFilter(Provider<MeterRegistry> meterRegistryProvider, SdkMetricsNettyClientFilterConfig sdkMetricsNettyClientFilterConfig) {
        this.meterRegistryProvider = meterRegistryProvider;
        this.sdkMetricsNettyClientFilterConfig = sdkMetricsNettyClientFilterConfig;
    }

    @Override
    public void beforeRequest(HttpRequest request, Map<String, Object> context) {
        if (sdkMetricsNettyClientFilterConfig.ignorePaths() != null && sdkMetricsNettyClientFilterConfig.ignorePaths().contains(request.uri().getPath())) {
            return;
        }

        Timer.Sample timerSample = Timer.start(meterRegistryProvider.get());
        context.put(METRICS_TIMER, timerSample);
    }

    @Override
    public HttpResponse afterResponse(HttpRequest request, HttpResponse response, Throwable throwable, Map<String, Object> context) {

        if (sdkMetricsNettyClientFilterConfig.ignorePaths() != null && sdkMetricsNettyClientFilterConfig.ignorePaths().contains(request.uri().getPath())) {
            return response;
        }

        Timer.Sample timerSample = (Timer.Sample) context.get(METRICS_TIMER);
        List<Tag> tags = new ArrayList<>();

        tags.add(method(request.method().name()));
        tags.add(serviceId("oci"));
        tags.add(exception(throwable));
        tags.add(uri(response, request.uri().getPath()));

        if (response != null) {
            tags.add(Tag.of(STATUS, String.valueOf(response.status())));
        }

        final Timer timer = Timer.builder(METRICS_NAME)
            .description("oci sdk client metrics")
            .tags(tags)
            .register(meterRegistryProvider.get());
        timerSample.stop(timer);
        return response;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    /**
     * Get a tag with the HTTP method name.
     *
     * @param httpMethod The name of the HTTP method.
     * @return Tag of method
     */
    private static Tag method(String httpMethod) {
        return httpMethod == null ? null : Tag.of(METHOD, httpMethod);
    }

    /**
     * Get a tag with the URI.
     *
     * @param httpResponse the HTTP response
     * @param path         the path of the request
     * @return Tag of URI
     */
    private static Tag uri(HttpResponse httpResponse, String path) {
        if (httpResponse != null) {
            int status = httpResponse.status();
            if (status >= 300 && status < 400) {
                return URI_REDIRECTION;
            }
            if (status >= 400 && status < 500) {
                if (status  == 401) {
                    return URI_UNAUTHORIZED;
                }
                if (status == 404) {
                    return URI_NOT_FOUND;
                }
                return URI_BAD_REQUEST;
            }
        }
        return Tag.of(URI, sanitizePath(path));
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

    /**
     * Get a tag with the serviceId used in the call.
     *
     * @param serviceId The serviceId used in the call.
     * @return Tag of serviceId
     */
    private static Tag serviceId(String serviceId) {
        return serviceId == null ? null : Tag.of(SERVICE_ID, serviceId);
    }

    /**
     * Sanitize the URI path for double slashes and ending slashes.
     *
     * @param path the URI of the request
     * @return sanitized string
     */
    private static String sanitizePath(String path) {
        if (!StringUtils.isEmpty(path)) {
            path = path
                .replaceAll("//+", "/")
                .replaceAll("/$", "");
        }

        return path != null ? (path.isEmpty() ? "root" : path) : UNKNOWN;
    }
}
