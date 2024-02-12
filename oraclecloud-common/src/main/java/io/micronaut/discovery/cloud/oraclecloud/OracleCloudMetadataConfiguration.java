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
package io.micronaut.discovery.cloud.oraclecloud;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.Toggleable;
import io.micronaut.runtime.ApplicationConfiguration;

/**
 * Default configuration for retrieving Oracle Cloud metadata for {@link io.micronaut.context.env.ComputePlatform#ORACLE_CLOUD}.
 *
 * @author Todd Sharp
 * @since 1.2.0
 */
@ConfigurationProperties(OracleCloudMetadataConfiguration.PREFIX)
@Requires(env = Environment.ORACLE_CLOUD)
@Requires(classes = ApplicationConfiguration.class)
@Primary
public class OracleCloudMetadataConfiguration implements Toggleable {

    /**
     * Prefix for Oracle Cloud configuration metadata.
     */
    public static final String PREFIX = ApplicationConfiguration.PREFIX + "." + Environment.ORACLE_CLOUD + ".metadata";

    /**
     * The default enable value.
     */
    @SuppressWarnings("WeakerAccess")
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * The default url value.
     */
    @SuppressWarnings("WeakerAccess")
    // CHECKSTYLE:OFF
    public static final String DEFAULT_URL = "http://169.254.169.254/opc/v1/instance/";
    public static final String DEFAULT_V2_URL = "http://169.254.169.254/opc/v2/instance/";

    public static final String DEFAULT_VNIC_URL = "http://169.254.169.254/opc/v1/vnics/";
    public static final String DEFAULT_V2_VNIC_URL = "http://169.254.169.254/opc/v2/vnics/";
    // CHECKSTYLE:ON

    private String url;
    private String vnicUrl = DEFAULT_VNIC_URL;
    private boolean enabled = DEFAULT_ENABLED;

    private boolean v1Enabled;
    private boolean v2Enabled = true;

    /**
     * @return Whether the Oracle Cloud configuration is enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Default value ({@value #DEFAULT_ENABLED}).
     * @param enabled Enable or disable the Oracle Cloud configuration
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return The Url
     */
    public String getUrl() {
        if (url == null) {
            return v2Enabled ? DEFAULT_V2_URL : DEFAULT_URL;
        }
        return url;
    }

    /**
     * Default value: {@value #DEFAULT_URL} or {@value #DEFAULT_V2_URL}, depending on the value of {@link #v2Enabled}.
     * @param url The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return The metadata Url
     * @deprecated Use {@link #getUrl()} instead.
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public String getMetadataUrl() {
        return getUrl();
    }

    /**
     * Deprecated. Use <code>url</code> instead.
     *
     * @param metadataUrl The metadata Url
     * @deprecated Use {@link #setUrl(String)} instead.
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public void setMetadataUrl(String metadataUrl) {
        setUrl(metadataUrl);
    }

    /**
     * @return The instance document Url
     * @deprecated Use {@link #getUrl()} instead.
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public String getInstanceDocumentUrl() {
        return getUrl();
    }

    /**
     * Deprecated. Use <code>url</code> instead.
     *
     * @param instanceDocumentUrl The instance document Url
     * @deprecated Use {@link #setUrl(String)} instead.
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public void setInstanceDocumentUrl(String instanceDocumentUrl) {
        setUrl(instanceDocumentUrl);
    }

    /**
     * Default value: {@value #DEFAULT_VNIC_URL} or {@value #DEFAULT_V2_VNIC_URL}, depending on the value of {@link #v2Enabled}.
     * @return The VNIC Url
     */
    public String getVnicUrl() {
        if (vnicUrl == null) {
            return v2Enabled ? DEFAULT_V2_VNIC_URL : DEFAULT_VNIC_URL;
        }
        return vnicUrl;
    }

    /**
     * @param vnicUrl The instance document Url
     */
    public void setVnicUrl(String vnicUrl) {
        this.vnicUrl = vnicUrl;
    }

    /**
     * @return Whether the V1 metadata is enabled
     */
    public boolean isV1Enabled() {
        return v1Enabled;
    }

    /**
     * Default value: <code>false</code>.
     * @param v1Enabled Enable or disable the V1 metadata
     */
    public void setV1Enabled(boolean v1Enabled) {
        this.v1Enabled = v1Enabled;
    }

    /**
     * @return Whether the V2 metadata is enabled
     */
    public boolean isV2Enabled() {
        return v2Enabled;
    }

    /**
     * Default value: <code>true</code>.
     * @param v2Enabled Enable or disable the V2 metadata
     */
    public void setV2Enabled(boolean v2Enabled) {
        this.v2Enabled = v2Enabled;
    }
}
