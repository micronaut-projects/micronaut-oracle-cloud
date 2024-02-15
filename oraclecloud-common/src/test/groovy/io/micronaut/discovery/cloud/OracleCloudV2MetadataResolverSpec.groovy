package io.micronaut.discovery.cloud

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.discovery.cloud.oraclecloud.OracleCloudInstanceMetadata
import io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataConfiguration
import io.micronaut.discovery.cloud.oraclecloud.OracleCloudMetadataResolver
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.RequestFilter
import io.micronaut.http.annotation.ServerFilter
import io.micronaut.http.filter.FilterContinuation
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import spock.lang.Specification

import java.nio.file.Paths

import static io.micronaut.discovery.cloud.OracleCloudMetadataResolverSpec.assertThatMetadataIsCorrect

class OracleCloudV2MetadataResolverSpec extends Specification {

    void "it can resolve metadata from a v2 endpoint"() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudV2MetadataResolverSpec",
                        (OracleCloudMetadataConfiguration.PREFIX + ".v2-enabled"): true,
                ], Environment.ORACLE_CLOUD) as EmbeddedServer
        def url = server.URL.toString()
        def ctx = server.applicationContext
        ctx.getBean(OracleCloudMetadataConfiguration).url = "${url}/opc/v2/instance"
        ctx.getBean(OracleCloudMetadataConfiguration).vnicUrl = "${url}/opc/v2/vnics"
        def resolver = ctx.getBean(OracleCloudMetadataResolver)
        def env = ctx.environment

        when:
        def metadata = resolver.resolve(env).get() as OracleCloudInstanceMetadata

        then:
        assertThatMetadataIsCorrect(metadata)

        cleanup:
        ctx.close()
        server.stop()
    }

    @Controller("/opc/v2")
    @Requires(property = "spec.name", value = "OracleCloudV2MetadataResolverSpec")
    static class InstanceMetadataServiceV2Mock {

        private final String instanceMetadata
        private final String instanceNetworkMetadata

        InstanceMetadataServiceV2Mock() {
            String currentPath = Paths.get("").toAbsolutePath().toString()
            instanceMetadata = new File("${currentPath}/src/test/groovy/io/micronaut/discovery/cloud/instanceMetadata.json").text
            instanceNetworkMetadata = new File("${currentPath}/src/test/groovy/io/micronaut/discovery/cloud/instanceNetworkMetadata.json").text
        }

        @Get("/instance")
        String instance() {
            return instanceMetadata
        }

        @Get("/vnics")
        String vnics() {
            return instanceNetworkMetadata
        }
    }

    @ServerFilter("/opc/v2/**")
    static class InstanceMetadataServiceV2Filter {

        @RequestFilter
        @ExecuteOn(TaskExecutors.BLOCKING)
        HttpResponse<?> checkAuthorization(HttpRequest<?> request, FilterContinuation<MutableHttpResponse<?>> continuation) {
            if (request.headers.authorization.isEmpty() || request.headers.authorization.get() != "Bearer Oracle") {
                HttpResponse.unauthorized()
            } else {
                continuation.proceed()
            }
        }
    }
}
