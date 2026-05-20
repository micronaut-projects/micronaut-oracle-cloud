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
package io.micronaut.oraclecloud.streaming;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;
import io.micronaut.oraclecloud.core.OracleCloudCoreFactory;

/**
 * Configuration for OCI Streaming Kafka compatibility.
 *
 * @since 6.0.0
 */
@ConfigurationProperties(OracleCloudStreamingConfiguration.PREFIX)
@Requires(property = OracleCloudStreamingConfiguration.PREFIX + ".stream-pool-id")
@Requires(property = OracleCloudStreamingConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE)
public class OracleCloudStreamingConfiguration implements Toggleable {
    /**
     * The OCI Streaming configuration prefix.
     */
    public static final String PREFIX = OracleCloudCoreFactory.ORACLE_CLOUD + ".streaming";

    private boolean enabled = true;
    private AuthenticationMode authMode;
    private String authToken;
    private String bootstrapServers;
    private String configFile;
    private String domainName;
    private String metadataBaseUrl;
    private String profile;
    private String region;
    private String streamPoolId;
    private String tenancyName;
    private String username;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether OCI Streaming Kafka configuration is enabled.
     *
     * @param enabled Whether configuration is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return The authentication mode
     */
    public AuthenticationMode getAuthMode() {
        return authMode;
    }

    /**
     * Sets the authentication mode. If unset, auth token mode is used when an auth token is configured,
     * otherwise user principal mode is used.
     *
     * @param authMode The authentication mode
     */
    public void setAuthMode(AuthenticationMode authMode) {
        this.authMode = authMode;
    }

    /**
     * @return The OCI auth token for {@link AuthenticationMode#AUTH_TOKEN}
     */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * @param authToken The OCI auth token for {@link AuthenticationMode#AUTH_TOKEN}
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    /**
     * @return Explicit Kafka bootstrap servers
     */
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    /**
     * @param bootstrapServers Explicit Kafka bootstrap servers
     */
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    /**
     * @return OCI SDK config file for {@link AuthenticationMode#USER_PRINCIPAL}
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * @param configFile OCI SDK config file for {@link AuthenticationMode#USER_PRINCIPAL}
     */
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * @return Identity domain name for auth-token usernames
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @param domainName Identity domain name for auth-token usernames
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * @return Instance metadata base URL for {@link AuthenticationMode#INSTANCE_PRINCIPAL}
     */
    public String getMetadataBaseUrl() {
        return metadataBaseUrl;
    }

    /**
     * @param metadataBaseUrl Instance metadata base URL for {@link AuthenticationMode#INSTANCE_PRINCIPAL}
     */
    public void setMetadataBaseUrl(String metadataBaseUrl) {
        this.metadataBaseUrl = metadataBaseUrl;
    }

    /**
     * @return OCI SDK profile for {@link AuthenticationMode#USER_PRINCIPAL}
     */
    public String getProfile() {
        return profile;
    }

    /**
     * @param profile OCI SDK profile for {@link AuthenticationMode#USER_PRINCIPAL}
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * @return OCI region ID used to derive bootstrap servers
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region OCI region ID used to derive bootstrap servers
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * @return OCI stream pool OCID
     */
    public String getStreamPoolId() {
        return streamPoolId;
    }

    /**
     * @param streamPoolId OCI stream pool OCID
     */
    public void setStreamPoolId(String streamPoolId) {
        this.streamPoolId = streamPoolId;
    }

    /**
     * @return Tenancy name for auth-token usernames
     */
    public String getTenancyName() {
        return tenancyName;
    }

    /**
     * @param tenancyName Tenancy name for auth-token usernames
     */
    public void setTenancyName(String tenancyName) {
        this.tenancyName = tenancyName;
    }

    /**
     * @return Full OCI Streaming Kafka username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username Full OCI Streaming Kafka username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Auth-token username parts configuration.
     */
    @ConfigurationProperties("")
    public static class AuthTokenUsernameConfiguration {
        private String userName;

        /**
         * @return User name for auth-token usernames
         */
        public String getUserName() {
            return userName;
        }

        /**
         * @param userName User name for auth-token usernames
         */
        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    /**
     * OCI Streaming Kafka authentication modes.
     */
    public enum AuthenticationMode {
        /**
         * Use a Kafka SASL/PLAIN username and OCI auth token.
         */
        AUTH_TOKEN,

        /**
         * Use OCI SDK instance principals.
         */
        INSTANCE_PRINCIPAL,

        /**
         * Use OCI SDK resource principals.
         */
        RESOURCE_PRINCIPAL,

        /**
         * Use an OCI SDK config file user principal.
         */
        USER_PRINCIPAL
    }
}
