package io.micronaut.oraclecloud.oke.kubernetes.client

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1NamespaceList
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

/**
 * This is a test that can be run with an actual OKE cluster.
 * Set {@code oci.oke.kubernetes.client.cluster-id} property and make sure you have a valid OCI configuration to run it.
 */
@Requires(bean = AuthenticationDetailsProvider.class)
@Requires(property = "oci.oke.kubernetes.client.cluster-id")
@Property(name = "oci.oke.kubernetes.client.endpoint-type", value = 'PublicEndpoint')
@Property(name = "kubernetes.client.enabled", value = '${oci.oke.kubernets.client.cluster-id:false}')
@MicronautTest
class ProductionKubernetesSpec extends Specification {

    @Inject
    CoreV1Api coreV1Api

    void "test kubernetes request"() {
        when:
        V1NamespaceList list = coreV1Api.listNamespace(
                null, null, null, null,
                null, null, null, null,
                null, null, null
        )

        then:
        list.getItems().size() >= 4
        list.getItems().collect { it.metadata.name }.toSorted() == ["default", "kube-node-lease", "kube-public", "kube-system"]
    }

}
