/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.oraclecloud.certificates;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

/**
 * Allows the configuration of the Oracle Cloud certificate process.
 */
@ConfigurationProperties("oci.certificates")
public class OracleCloudCertificationsConfiguration implements Toggleable {

    private static final boolean DEFAULT_ORACLE_CLOUD_CERT_ENABLED = true;
    private boolean enabled = DEFAULT_ORACLE_CLOUD_CERT_ENABLED;
    private String certificateId;
    private Long versionNumber;
    private String certificateVersionName;

    /**
     * Certificate Version number on Oracle Cloud Certificate service.
     *
     * @return version number from certificate
     */
    public Long getVersionNumber() {
        return versionNumber;
    }

    /**
     * Sets version number of certificate from Oracle Cloud Certificate service.
     *
     * @param versionNumber The version number
     */
    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Certificate name from Oracle Cloud Certificate service.
     *
     * @return certificate name
     */
    public String getCertificateVersionName() {
        return certificateVersionName;
    }

    /**
     * Sets name of certificate from Oracle Cloud Certificate service.
     *
     * @param certificateVersionName The certificate name
     */
    public void setCertificateVersionName(String certificateVersionName) {
        this.certificateVersionName = certificateVersionName;
    }

    /**
     * Certificate ocid from Oracle Cloud Certificate service.
     *
     * @return ocid of certificate
     */
    public String getCertificateId() {
        return certificateId;
    }

    /**
     * Sets ocid of certificate from Oracle Cloud Certificate Service.
     *
     * @param certificateId The certificate ocid.
     */
    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    /**
     * If Oracle Cloud certificate background and setup process should be enabled.
     *
     * @return True if Oracle Cloud certificate process is enabled.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets if Oracle Cloud certificate background and setup process is enabled. Default {@value #DEFAULT_ORACLE_CLOUD_CERT_ENABLED}.
     *
     * @param enabled The enablement flag
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
