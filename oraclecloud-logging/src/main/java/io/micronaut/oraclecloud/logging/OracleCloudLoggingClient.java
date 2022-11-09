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

import com.oracle.bmc.loggingingestion.Logging;
import com.oracle.bmc.loggingingestion.requests.PutLogsRequest;
import com.oracle.bmc.loggingingestion.responses.PutLogsResponse;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.annotation.Internal;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import java.util.Optional;

/**
 * OracleCloudLoggingClient is a {@link Logging} client that is required for {@link OracleCloudAppender}.
 *
 * @author Nemanja Mikic
 * @since 2.2.0
 */
@Context
@Internal
@Singleton
final class OracleCloudLoggingClient implements ApplicationEventListener<ServerStartupEvent> {

    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".logging";

    private static Logging logging;
    private static String host;
    private static String appName;
    private static String logId;
    private final Logging internalLogging;
    private final String internalAppName;
    private final String internalLogId;

    public OracleCloudLoggingClient(
            Logging logging, ApplicationConfiguration applicationConfiguration,
            @Property(name = PREFIX + ".logId")
     Optional<String> internalLogId) {
        this.internalLogging = logging;
        this.internalAppName = applicationConfiguration.getName().orElse("");
        this.internalLogId = internalLogId.orElse(null);
    }

    static synchronized boolean isReady() {
        return logging != null;
    }

    static synchronized String getHost() {
        return host;
    }

    static synchronized String getAppName() {
        return appName;
    }

    private static synchronized void setLogging(Logging logging, String host, String appName, String logId) {
        OracleCloudLoggingClient.logging = logging;
        OracleCloudLoggingClient.host = host;
        OracleCloudLoggingClient.appName = appName;
        OracleCloudLoggingClient.logId = logId;
    }

    static synchronized void destroy() throws Exception {
        OracleCloudLoggingClient.logging.close();
        OracleCloudLoggingClient.logging = null;
        OracleCloudLoggingClient.host = null;
        OracleCloudLoggingClient.appName = null;
    }

    static synchronized boolean putLogs(PutLogsRequest putLogsRequest) {
        if (logging != null) {
            PutLogsResponse putLogsResponse = logging.putLogs(putLogsRequest);
            return putLogsResponse.getOpcRequestId() != null;
        }
        return false;
    }

    public static synchronized String getLogId() {
        return logId;
    }

    @PreDestroy
    public void close() throws Exception {
        OracleCloudLoggingClient.destroy();
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        setLogging(internalLogging, event.getSource().getHost(), internalAppName, internalLogId);
    }
}
