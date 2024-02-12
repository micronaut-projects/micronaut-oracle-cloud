/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.discovery.cloud

import io.micronaut.context.env.ComputePlatform
import io.micronaut.context.env.Environment
import io.micronaut.discovery.cloud.oraclecloud.OracleCloudInstanceMetadata
import io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataConfiguration
import io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataResolver
import io.micronaut.json.JsonMapper
import spock.lang.Specification

import java.nio.file.Paths


class OracleCloudMetadataResolverSpec extends Specification {

    void "test building oracle cloud compute metadata"() {
        given:
        Environment environment = Mock(Environment)
        OracleCloudMetadataResolver resolver = buildResolver()
        Optional<OracleCloudInstanceMetadata> computeInstanceMetadata = resolver.resolve(environment) as Optional<OracleCloudInstanceMetadata>

        expect:
        computeInstanceMetadata.isPresent()
        def metadata = computeInstanceMetadata.get()
        assertThatMetadataIsCorrect(metadata)
    }

    static void assertThatMetadataIsCorrect(OracleCloudInstanceMetadata m) {
        assert m.computePlatform == ComputePlatform.ORACLE_CLOUD
        assert m.faultDomain == "FAULT-DOMAIN-2"
        assert m.region == "us-phoenix-1"
        assert m.availabilityZone == "GTEq:PHX-AD-3"
        assert m.name == "micronaut-env"
        assert m.machineType == "VM.Standard.A1.Flex"
        assert m.instanceId == "ocid1.instance.oc1.phx.redacted"
        assert m.imageId == "ocid1.image.oc1.phx.redacted"

        assert m.interfaces.size() == 1
        NetworkInterface i = m.interfaces.first()
        assert i.id == "ocid1.vnic.oc1.phx.abyhqljrdpeodblmzaipwmdgusajz7a5rlbd7xp6k7s4nq74h3ozrg3svvhq"
        assert i.ipv4 == "10.0.0.19"
        assert i.mac == "02:00:17:03:1C:BA"

        assert m.metadata['timeCreated'] == "1696536074936"
        assert m.metadata['monitoringDisabled'] == "false"
        assert m.metadata['region'] == "us-phoenix-1"
        assert m.metadata['zone'] == "GTEq:PHX-AD-3"
        assert m.metadata['compute_management.instance_configuration.state'] == "SUCCEEDED"
        assert m.metadata['hostclass'] == "hostclass"
        assert m.metadata['ssh_authorized_keys'] == "ssh-rsa redacted"

        assert m.tags.size() == 5
        assert m.tags['CreatedBy'] == "ocid1.flock.oc1..redacted"
        assert m.tags['CreatedOn'] == "2023-10-05T17:57:37.163Z"
        assert m.tags['oci:compute:instanceconfiguration'] == "ocid1.instanceconfiguration.oc1.phx.redacted"
        assert m.tags['oci:compute:instancepool'] == "ocid1.instancepool.oc1.phx.redacted"
        assert m.tags['oci:compute:instancepool:opcretrytoken'] == "redacted"
    }

    private OracleCloudMetadataResolver buildResolver() {
        def configuration = new OracleCloudMetadataConfiguration()
        String currentPath = Paths.get("").toAbsolutePath().toString()
        configuration.url = "file:///${currentPath}/src/test/groovy/io/micronaut/discovery/cloud/instanceMetadata.json"
        configuration.vnicUrl = "file:///${currentPath}/src/test/groovy/io/micronaut/discovery/cloud/instanceNetworkMetadata.json"
        return new OracleCloudMetadataResolver(JsonMapper.createDefault(), configuration)
    }
}
