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
package io.micronaut.oraclecloud.notifications;

import com.oracle.bmc.ons.NotificationDataPlane;
import io.micronaut.core.annotation.Internal;

/**
 * Creates OCI Notifications data plane clients for resolved topic endpoints.
 *
 * @since 6.0.0
 */
@Internal
public interface NotificationDataPlaneFactory {
    /**
     * @param endpoint The resolved topic API endpoint.
     * @return A data plane client configured for the endpoint.
     */
    NotificationDataPlane create(String endpoint);
}
