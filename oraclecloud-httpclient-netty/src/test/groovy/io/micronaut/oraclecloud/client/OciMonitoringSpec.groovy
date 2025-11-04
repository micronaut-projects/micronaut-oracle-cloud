package io.micronaut.oraclecloud.client

import com.oracle.bmc.Service
import com.oracle.bmc.Services
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.RegionProvider
import com.oracle.bmc.monitoring.MonitoringClient
import com.oracle.bmc.monitoring.model.Datapoint
import com.oracle.bmc.monitoring.model.MetricDataDetails
import com.oracle.bmc.monitoring.model.PostMetricDataDetails
import com.oracle.bmc.monitoring.model.SummarizeMetricsDataDetails
import com.oracle.bmc.monitoring.requests.PostMetricDataRequest
import com.oracle.bmc.monitoring.requests.SummarizeMetricsDataRequest
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant

import static java.time.temporal.ChronoUnit.MINUTES

@Requires(property = "monitoring.compartment.ocid")
@Requires(bean = AuthenticationDetailsProvider)
@MicronautTest
@Property(name = "use.real.auth", value = "true")
class OciMonitoringSpec extends Specification {

    private static final Map<String, String> DIMENSIONS = [host: 'some-host']
    private static final String NAMESPACE = 'micronaut_test'

    @Property(name = "monitoring.compartment.ocid")
    String compartmentId

    @Inject
    @NonNull
    AuthenticationDetailsProvider authenticationDetailsProvider

    @Inject
    RegionProvider regionProvider

    void "test post metric data"() {
        given:
        var metricData = createMetricData("test.metric", [
                datapoint(1.5, 1),
                datapoint(0.7, 3)
        ])

        when:
        var response = createTelemetryClient().postMetricData(
                PostMetricDataRequest.builder()
                        .postMetricDataDetails(metricData)
                        .build())

        then:
        response.postMetricDataResponseDetails.failedMetricsCount == 0
    }

    void "test summarize metrics"() {
        given:
        Date startTime = Date.from(Instant.now().minus(5, MINUTES))
        String name = "test.metric." + new Random().nextInt(0, Integer.MAX_VALUE)
        var metricData = createMetricData(name, [
                datapoint(1, 1),
                datapoint(3, 1)
        ])

        when:
        var postResponse = createTelemetryClient().postMetricData(
                PostMetricDataRequest.builder()
                        .postMetricDataDetails(metricData)
                        .build())

        then:
        postResponse.postMetricDataResponseDetails.failedMetricsCount == 0

        when:
        var builder = SummarizeMetricsDataDetails.builder()
                .namespace(NAMESPACE)
                .query(name + "[1m].mean()")
                .startTime(startTime)
        var client = createClient()
        PollingConditions conditions = new PollingConditions(timeout: 120, initialDelay: 15, delay: 15)

        then:
        conditions.eventually {
            var response = client.summarizeMetricsData(
                    SummarizeMetricsDataRequest.builder()
                            .compartmentId(compartmentId)
                            .summarizeMetricsDataDetails(builder.endTime(new Date()).build())
                            .build())
            response.__httpStatusCode__ == 200
            response.items.size() == 1
            response.items[0].aggregatedDatapoints.size() == 1
            response.items[0].aggregatedDatapoints[0].value == 2.0
            response.items[0].compartmentId == compartmentId
            response.items[0].name == name
            response.items[0].namespace == NAMESPACE
            response.items[0].dimensions == DIMENSIONS
        }
    }

    private MonitoringClient createClient() {
        MonitoringClient.builder().build(authenticationDetailsProvider)
    }

    private MonitoringClient createTelemetryClient() {
        Service service = Services.serviceBuilder().serviceName("MONITORING-INGESTION")
                .serviceEndpointPrefix("telemetry-ingestion")
                .serviceEndpointTemplate("https://telemetry-ingestion.{region}.{secondLevelDomain}")
                .build()
        return MonitoringClient.builder()
                .endpoint(regionProvider.region.getEndpoint(service).orElseThrow())
                .build(authenticationDetailsProvider)
    }

    private PostMetricDataDetails createMetricData(String name, List<Datapoint> datapoints) {
        PostMetricDataDetails.builder()
                .metricData([
                        MetricDataDetails.builder()
                                .name(name)
                                .compartmentId(compartmentId)
                                .namespace(NAMESPACE)
                                .datapoints(datapoints)
                                .dimensions(DIMENSIONS)
                                .build()
                ])
                .build()
    }

    private static Datapoint datapoint(double value, int count) {
        Datapoint.builder().timestamp(new Date()).value(value).count(count).build()
    }
}
