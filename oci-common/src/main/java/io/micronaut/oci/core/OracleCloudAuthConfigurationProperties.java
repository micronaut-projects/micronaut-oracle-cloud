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

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Configuration for Oracle Cloud auth config.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@ConfigurationProperties(OracleCloudCoreFactory.ORACLE_CLOUD)
@Requires(property = OracleCloudAuthConfigurationProperties.TENANT_ID)
public class OracleCloudAuthConfigurationProperties {
    public static final String TENANT_ID = OracleCloudCoreFactory.ORACLE_CLOUD + ".tenant-id";

    private Readable privateKeyFile;
    private String privateKey;

    @ConfigurationBuilder(prefixes = "", excludes = "privateKeySupplier")
    private final SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder builder = SimpleAuthenticationDetailsProvider.builder();

    /**
     * @return The builder.
     */
    public SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder getBuilder() {
        if (privateKeyFile != null) {
            builder.privateKeySupplier(() -> {
                try {
                    return privateKeyFile.asInputStream();
                } catch (IOException e) {
                    throw new ConfigurationException("Invalid Oracle Cloud private key specified");
                }
            });
        }
        if (privateKey != null) {
            builder.privateKeySupplier(() -> new ByteArrayInputStream(privateKey.getBytes()));
        }
        return builder;
    }


    /**
     * @param privateKeyFile The private key location.
     */
    public void setPrivateKeyFile(Readable privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    /**
     * @param privateKey The private key as a string
     */
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
