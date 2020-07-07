/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oci.core;

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.Readable;

import java.io.IOException;

/**
 * Configuration for Oracle Cloud auth config.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@ConfigurationProperties(OracleCloudAuthConfigurationProperties.PREFIX)
@Requires(property = OracleCloudAuthConfigurationProperties.PREFIX)
public class OracleCloudAuthConfigurationProperties {
    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".auth";

    private Readable privateKey;

    @ConfigurationBuilder(prefixes = "")
    private SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder builder = SimpleAuthenticationDetailsProvider.builder();

    /**
     * @return The builder.
     */
    public SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder getBuilder() {
        if (privateKey != null) {
            builder.privateKeySupplier(() -> {
                try {
                    return privateKey.asInputStream();
                } catch (IOException e) {
                    throw new ConfigurationException("Invalid Oracle Cloud private key specified");
                }
            });
        }
        return builder;
    }

    /**
     * @return The private key
     */
    public Readable getPrivateKey() {
        return privateKey;
    }

    /**
     * @param privateKey The private key location.
     */
    public void setPrivateKey(Readable privateKey) {
        this.privateKey = privateKey;
    }
}
