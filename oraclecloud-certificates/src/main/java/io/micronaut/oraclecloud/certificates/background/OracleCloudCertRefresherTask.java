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

import io.micronaut.context.annotation.Requires;
import io.micronaut.oraclecloud.certificates.OracleCloudCertificationsConfiguration;
import io.micronaut.oraclecloud.certificates.services.OracleCloudCertificateService;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.exceptions.ApplicationStartupException;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background task to automatically refresh the certificates from an Oracle Cloud Certificate server on a configurable interval.
 */
@Singleton
@Requires(property = OracleCloudCertificationsConfiguration.PREFIX + ".enabled", value = "true")
public final class OracleCloudCertRefresherTask {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudCertRefresherTask.class);

    private final OracleCloudCertificateService oracleCloudCertificateService;

    /**
     * Constructs a new Oracle Certificate cert refresher background task.
     *
     * @param oracleCloudCertificateService       Oracle Cloud Certificate service
     */
    public OracleCloudCertRefresherTask(OracleCloudCertificateService oracleCloudCertificateService) {
        this.oracleCloudCertificateService = oracleCloudCertificateService;
    }

    /**
     * Scheduled task to refresh certs from Oracle Cloud Certificate server.
     *
     */
    @Scheduled(
            fixedDelay = "${oci.certificates.refresh.frequency:24h}",
            initialDelay = "${oci.certificates.refresh.delay:24h}")
    void backgroundRenewal() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running background/scheduled renewal process");
        }
        refreshCertificate();
    }

    /**
     * Checks to see if certificate needs renewed on app startup.
     *
     * @param startupEvent Startup event
     */
    @EventListener
    void onStartup(ApplicationStartupEvent startupEvent) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Running startup renewal process");
            }
            refreshCertificate();
        } catch (Exception e) { //NOSONAR
            LOG.error("Failed to initialize certificate for SSL no requests would be secure. Stopping application", e);
            throw new ApplicationStartupException("Failed to start due to SSL configuration issue.", e);
        }
    }

    /**
     * Does the work to actually renew the certificate if it needs to be done.
     */
    public void refreshCertificate() {
        oracleCloudCertificateService.refreshCertificate();
    }
}
