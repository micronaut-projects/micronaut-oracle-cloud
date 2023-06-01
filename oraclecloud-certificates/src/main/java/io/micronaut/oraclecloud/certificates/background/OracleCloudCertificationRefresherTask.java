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
package io.micronaut.oraclecloud.certificates.background;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration;
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background task to automatically refresh the certificates from an Oracle Cloud Certificate server on a configurable interval.
 */
@Singleton
@Requires(property = OracleCloudCertificationsConfiguration.PREFIX + ".enabled", value = "true")
@Context
@Internal
public final class OracleCloudCertificationRefresherTask {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudCertificationRefresherTask.class);

    private final OracleCloudCertificateService oracleCloudCertificateService;

    /**
     * Constructs a new Oracle Certificate cert refresher background task.
     *
     * @param oracleCloudCertificateService       Oracle Cloud Certificate service
     */
    public OracleCloudCertificationRefresherTask(OracleCloudCertificateService oracleCloudCertificateService) {
        this.oracleCloudCertificateService = oracleCloudCertificateService;
        oracleCloudCertificateService.refreshCertificate();
    }

    /**
     * Scheduled task to refresh certs from Oracle Cloud Certificate server.
     *
     */
    @Scheduled(
            fixedDelay = "${oci.certificates.refresh.frequency:24h}",
            initialDelay = "${oci.certificates.refresh.delay:24h}")
    public void backgroundRenewal() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running background/scheduled renewal process");
        }
        oracleCloudCertificateService.refreshCertificate();
    }
}
