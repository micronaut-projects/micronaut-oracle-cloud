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
package io.micronaut.oraclecloud.httpclient.apache.core;

import com.oracle.bmc.http.client.ClientProperty;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Path;

/**
 * OCI java SDK HTTP provider based on Apache HTTP components core, sending all requests to a local
 * unix domain socket. The serialization backend is either jackson or micronaut-serialization,
 * depending on what is on the classpath.
 *
 * @author Jonas Konrad
 */
@Internal
@Singleton
@BootstrapContextCompatible
public final class ApacheCoreHttpProvider implements HttpProvider {
    public static final ClientProperty<Path> SOCKET_PATH = ClientProperty.create("socketPath");

    private final ApacheCoreSerializer serializer;

    public ApacheCoreHttpProvider() {
        // SPI constructor
        ApacheCoreSerializer s;
        try {
            s = new JacksonSerializer();
        } catch (LinkageError e1) {
            try {
                s = new SerdeSerializer();
            } catch (LinkageError e2) {
                e1.addSuppressed(e2);
                throw new IllegalStateException("No serializer implementation available. Please add a dependency on jackson-databind (and jackson-datatype-jsr310), or on a micronaut-serialization implementation.");
            }
        }
        serializer = s;
    }

    @Inject
    ApacheCoreHttpProvider(ApacheCoreSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public HttpClientBuilder newBuilder() {
        return new ApacheCoreHttpClientBuilder(this);
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    @Override
    public ApacheCoreSerializer getSerializer() {
        return serializer;
    }
}
