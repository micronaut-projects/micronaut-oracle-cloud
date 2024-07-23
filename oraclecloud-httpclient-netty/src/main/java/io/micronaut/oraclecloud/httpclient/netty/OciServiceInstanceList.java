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
package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.discovery.ServiceInstanceList;
import io.micronaut.http.client.loadbalance.DiscoveryClientLoadBalancerFactory;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;

/**
 * This is necessary to get the {@link ManagedNettyHttpProvider} working in the bootstrap context.
 */
@BootstrapContextCompatible
@Singleton
@Internal
@Requires(missingBeans = DiscoveryClientLoadBalancerFactory.class)
final class OciServiceInstanceList implements ServiceInstanceList {
    @Override
    public String getID() {
        return ManagedNettyHttpProvider.SERVICE_ID;
    }

    @Override
    public List<ServiceInstance> getInstances() {
        return Collections.emptyList();
    }
}
