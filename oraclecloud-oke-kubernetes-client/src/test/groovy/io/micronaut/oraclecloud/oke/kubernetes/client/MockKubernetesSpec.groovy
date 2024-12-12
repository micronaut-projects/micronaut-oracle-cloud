package io.micronaut.oraclecloud.oke.kubernetes.client

import com.oracle.bmc.Service
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.auth.AuthCachingPolicy
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.containerengine.ContainerEngineClient
import com.oracle.bmc.http.signing.RequestSigner
import com.oracle.bmc.http.signing.RequestSignerFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.HttpServerException
import io.micronaut.kubernetes.client.openapi.api.CoreV1Api
import io.micronaut.kubernetes.client.openapi.model.V1Namespace
import io.micronaut.kubernetes.client.openapi.model.V1NamespaceList
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MockKubernetesSpec extends Specification {

    @AutoCleanup
    EmbeddedServer server = ApplicationContext.run(EmbeddedServer, [
            'spec.name': 'MockKubernetesSpec-Server',
            'kubernetes.client.enabled': false
    ])

    void "test kubernetes request"() {
        given:
        var clientContext = ApplicationContext.run([
                "oci.oke.kubernetes.client.cluster-id": 'test-id',
                'spec.name': 'MockKubernetesSpec',
                'oci.config.enabled': 'false',
                "spec.server.url": server.URI
        ])
        var coreV1Api = clientContext.getBean(CoreV1Api)

        when:
        V1NamespaceList list = coreV1Api.listNamespace(
                null, null, null, null,
                null, null, null, null,
                null, null, null
        )

        then:
        list.getItems().size() == 1
        list.getItems()[0].metadata.name == 'my-test-name-1'

        cleanup:
        clientContext.close()
    }

    @Controller
    @Requires(property = 'spec.name', value = 'MockKubernetesSpec-Server')
    static class KubernetesController {

        static final String EXPECTED_AUTH = "https://containerengine.us-phoenix-1.oraclecloud.com/cluster_request/test-cluster-id?authorization=test&date=2024-02-12"

        static final String KUBE_CONFIG = """\
apiVersion: v1
kind: Config
clusters:
  - name: test-cluster
    cluster:
      server: %s
users:
  - name: test-user
    user:
      exec:
        apiVersion: client.authentication.k8s.io/v1beta1
        command: oci
        args:
          - ce
          - cluster
          - generate-token
          - --cluster-id
          - test-cluster-id
          - --region
          - us-phoenix-1
contexts:
  - name: test-context
    context:
      cluster: test-cluster
      user: test-user
current-context: test-context
"""

        private EmbeddedServer server

        KubernetesController(EmbeddedServer server) {
            this.server = server
        }

        // The container engine endpoint
        @Post("/20180222/clusters/test-id/kubeconfig/content")
        @Produces("application/x-yaml")
        String getKubeConfig() {
            return String.format(KUBE_CONFIG, server.getURI())
        }

        // The kubernetes endpoint
        @Get("/api/v1/namespaces")
        V1NamespaceList namespaces(@Header("authorization") auth) {
            var content = new String(Base64.getUrlDecoder().decode(auth.substring("Bearer ".length())), StandardCharsets.UTF_8)
            if (content != EXPECTED_AUTH) {
                throw new HttpServerException("Incorrect auth")
            }
            return new V1NamespaceList([
                    new V1Namespace().metadata(new V1ObjectMeta().name("my-test-name-1"))
            ])
        }
    }

    @Singleton
    @Requires(property = 'spec.name', value = 'MockKubernetesSpec')
    static class ClientConfigurator implements BeanCreatedEventListener<ContainerEngineClient.Builder> {

        private String serverUrl

        ClientConfigurator(@Property(name = "spec.server.url") String serverUrl) {
            this.serverUrl = serverUrl
        }

        @Override
        ContainerEngineClient.Builder onCreated(@NonNull BeanCreatedEvent<ContainerEngineClient.Builder> event) {
            event.getBean().endpoint(serverUrl)
            return event.getBean()
        }
    }

    @AuthCachingPolicy(cacheKeyId = false, cachePrivateKey = false)
    @Singleton
    @Replaces(ConfigFileAuthenticationDetailsProvider.class)
    @Requires(property = 'spec.name', value = 'MockKubernetesSpec')
    static class MockAuthenticationDetailsProvider implements AbstractAuthenticationDetailsProvider {

    }

    @Singleton
    @Requires(property = 'spec.name', value = 'MockKubernetesSpec')
    static class MockRequestSignerFactory implements RequestSignerFactory {
        @Override
        RequestSigner createRequestSigner(Service service, AbstractAuthenticationDetailsProvider abstractAuthenticationDetailsProvider) {
            return (URI uri, String s, Map<String, List<String>> map, Object o) -> [
                    'authorization': 'test',
                    'date': '2024-02-12'
            ]
        }
    }

}
