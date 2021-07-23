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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.cloud.ComputeInstanceMetadata;
import io.micronaut.discovery.cloud.ComputeInstanceMetadataResolver;
import io.micronaut.discovery.cloud.NetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.discovery.cloud.ComputeInstanceMetadataResolverUtils.populateMetadata;
import static io.micronaut.discovery.cloud.ComputeInstanceMetadataResolverUtils.readMetadataUrl;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.AGENT_CONFIG;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.AVAILABILITY_DOMAIN;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.CANONICAL_REGION_NAME;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.DISPLAY_NAME;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.ID;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.IMAGE;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.MAC;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.MONITORING_DISABLED;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.PRIVATE_IP;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.REGION;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.SHAPE;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.TIME_CREATED;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.USER_METADATA;
import static io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataKeys.VNIC_ID;

/**
 * Resolves {@link ComputeInstanceMetadata} for Oracle Cloud Infrastructure.
 *
 * @author Todd Sharp
 * @since 1.2.0
 */
@Singleton
@Requires(env = Environment.ORACLE_CLOUD)
@Requires(
        property = OracleCloudMetadataConfiguration.PREFIX + ".enabled",
        value = StringUtils.TRUE,
        defaultValue = StringUtils.TRUE)
@Requires(classes = ComputeInstanceMetadataResolver.class)
@Primary
public class OracleCloudMetadataResolver implements ComputeInstanceMetadataResolver {

    private static final Logger LOG = LoggerFactory.getLogger(OracleCloudMetadataResolver.class);
    private static final int READ_TIMEOUT_IN_MILLS = 5000;
    private static final int CONNECTION_TIMEOUT_IN_MILLS = 5000;

    private final ObjectMapper objectMapper;
    private final OracleCloudMetadataConfiguration configuration;
    private OracleCloudInstanceMetadata cachedMetadata;

    /**
     *
     * @param objectMapper To read and write JSON
     * @param configuration Oracle Cloud Metadata configuration
     */
    @Inject
    public OracleCloudMetadataResolver(ObjectMapper objectMapper, OracleCloudMetadataConfiguration configuration) {
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    /**
     * Construct with default settings.
     */
    public OracleCloudMetadataResolver() {
        objectMapper = new ObjectMapper();
        configuration = new OracleCloudMetadataConfiguration();
    }

    @Override
    public Optional<ComputeInstanceMetadata> resolve(Environment environment) {
        if (!configuration.isEnabled()) {
            return Optional.empty();
        }
        if (cachedMetadata != null) {
            cachedMetadata.setCached(true);
            return Optional.of(cachedMetadata);
        }

        OracleCloudInstanceMetadata instanceMetadata = new OracleCloudInstanceMetadata();

        try {
            String metadataUrl = configuration.getUrl();
            JsonNode metadataJson = readMetadataUrl(new URL(metadataUrl), CONNECTION_TIMEOUT_IN_MILLS, READ_TIMEOUT_IN_MILLS, objectMapper, new HashMap<>());
            if (metadataJson != null) {
                instanceMetadata.setInstanceId(textValue(metadataJson, ID));
                instanceMetadata.setName(textValue(metadataJson, DISPLAY_NAME));
                instanceMetadata.setRegion(textValue(metadataJson, CANONICAL_REGION_NAME));
                instanceMetadata.setAvailabilityZone(textValue(metadataJson, AVAILABILITY_DOMAIN));
                instanceMetadata.setImageId(textValue(metadataJson, IMAGE));
                instanceMetadata.setMachineType(textValue(metadataJson, SHAPE));

                Map<String, String> metadata = objectMapper.convertValue(metadataJson, Map.class);

                JsonNode agentConfig = metadataJson.findValue(AGENT_CONFIG.getName());
                metadata.put("timeCreated", textValue(metadataJson, TIME_CREATED));
                metadata.put("monitoringDisabled", textValue(agentConfig, MONITORING_DISABLED));
                JsonNode userMeta = metadataJson.findValue(USER_METADATA.getName());
                Iterator<String> userMetaFieldNames = userMeta.fieldNames();
                userMeta.forEach(userNode -> {
                    String fieldName = userMetaFieldNames.next();
                    metadata.put(fieldName, userNode.asText());
                });
                // override the 'region' in metadata in favor of canonicalRegionName
                metadata.put(REGION.getName(), textValue(metadataJson, CANONICAL_REGION_NAME));
                metadata.put("zone", textValue(metadataJson, AVAILABILITY_DOMAIN));

                populateMetadata(instanceMetadata, metadata);
            }

            String vnicUrl = configuration.getVnicUrl();
            JsonNode vnicJson = readMetadataUrl(
                    new URL(vnicUrl),
                    CONNECTION_TIMEOUT_IN_MILLS,
                    READ_TIMEOUT_IN_MILLS,
                    objectMapper,
                    new HashMap<>());

            if (vnicJson != null) {
                List<NetworkInterface> networkInterfaces = new ArrayList<>();
                vnicJson.elements().forEachRemaining(vnicNode -> {
                    OracleCloudNetworkInterface networkInterface = new OracleCloudNetworkInterface();
                    networkInterface.setId(textValue(vnicJson, VNIC_ID));
                    networkInterface.setIpv4(textValue(vnicJson, PRIVATE_IP));
                    networkInterface.setMac(textValue(vnicJson, MAC));
                    networkInterfaces.add(networkInterface);
                });
                instanceMetadata.setInterfaces(networkInterfaces);
            }

            cachedMetadata = instanceMetadata;
            return Optional.of(instanceMetadata);

        } catch (MalformedURLException mue) {
            LOG.error("Oracle Cloud metadataUrl value is invalid!: {}", configuration.getUrl(), mue);
        } catch (IOException ioe) {
            LOG.error("Error connecting to {} reading instance metadata", configuration.getUrl(), ioe);
        }

        return Optional.empty();
    }

    private String textValue(JsonNode node, OracleCloudMetadataKeys key) {
        JsonNode value = node.findValue(key.getName());
        if (value != null) {
            return value.asText();
        } else {
            return null;
        }
    }

}
