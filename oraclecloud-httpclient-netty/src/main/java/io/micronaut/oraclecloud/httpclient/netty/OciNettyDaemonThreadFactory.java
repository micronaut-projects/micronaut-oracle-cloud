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
package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.core.annotation.Internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Daemon thread factory for unmanaged OCI Netty HTTP client resources.
 */
@Internal
public final class OciNettyDaemonThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger();
    private final String namePrefix;

    public OciNettyDaemonThreadFactory() {
        this("oci-netty-http-client");
    }

    OciNettyDaemonThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, namePrefix + '-' + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }
}
