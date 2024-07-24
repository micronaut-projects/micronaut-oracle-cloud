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
package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.order.Ordered;

/**
 * OciNettyClientFilter interface allows invoking beforeRequest method before OCI SDK client sends a request and afterResponse invokes after request is sent and response is received from the server.
 *
 * @param <R> context object that will be passed from beforeRequest to afterResponse.
 *
 * @since 4.2.0
 * @author Nemanja Mikic
 */
@Indexed(OciNettyClientFilter.class)
public interface OciNettyClientFilter<R> extends Ordered {

    /**
    * The OCI SDK client will execute this method before sending request.
    * @param request the http requests.
    */
    @Nullable R beforeRequest(@NonNull HttpRequest request);

    /**
     * The OCI SDK client will execute this method after it receives the response or exception is thrown.
     * @param request the http requests.
     * @param response the http response.
     * @param throwable the exception that has been thrown during sending request and receiving response.
     * @param context the data passed from {@link OciNettyClientFilter#beforeRequest(HttpRequest)}
     */
    @Nullable HttpResponse afterResponse(@NonNull HttpRequest request, @Nullable HttpResponse response, @Nullable Throwable throwable, @Nullable R context);

}
