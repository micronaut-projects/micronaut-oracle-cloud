package io.micronaut.oraclecloud.client

import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
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

import java.time.Instant
import java.time.temporal.ChronoUnit

@Requires(property = "monitoring.compartment.ocid")
@Requires(bean = AuthenticationDetailsProvider)
@MicronautTest
class OciMonitoringSpec extends Specification {

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
                Datapoint.builder().timestamp(new Date()).value(1.5).count(1).build(),
                Datapoint.builder().timestamp(new Date()).value(0.7).count(3).build()
        ])

        when:
        var client = createTelemetryClient()
        var response = client.postMetricData(PostMetricDataRequest.builder().postMetricDataDetails(metricData).build())

        then:
        response.postMetricDataResponseDetails.failedMetricsCount == 0
    }

    void "test summarize metrics"() {
        when:
        var name = "test.metric." + new Random().nextInt(0, Integer.MAX_VALUE)
        var metricData = createMetricData(name, [
                Datapoint.builder().timestamp(new Date()).value(1).count(1).build(),
                Datapoint.builder().timestamp(new Date()).value(3).count(1).build()
        ])
        var postClient = createTelemetryClient()
        var postResponse = postClient.postMetricData(PostMetricDataRequest.builder().postMetricDataDetails(metricData).build())

        then:
        postResponse.postMetricDataResponseDetails.failedMetricsCount == 0

        when:
        Thread.sleep(30_000)
        var body = SummarizeMetricsDataDetails.builder()
            .namespace("micronaut_test")
            .query(name + "[1m].mean()")
            .startTime(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)))
            .endTime(new Date())
            .build()
        var client = createClient()
        var response = client.summarizeMetricsData(
                SummarizeMetricsDataRequest.builder().compartmentId(compartmentId).summarizeMetricsDataDetails(body).build()
        )

        then:
        response.__httpStatusCode__ == 200
        response.items.size() == 1
        response.items[0].aggregatedDatapoints.size() == 1
        response.items[0].aggregatedDatapoints[0].value == 2.0
        response.items[0].compartmentId == compartmentId
        response.items[0].name == name
        response.items[0].namespace == "micronaut_test"
        response.items[0].dimensions == ["host": "some-host"]
    }

    MonitoringClient createClient() {
        return MonitoringClient.builder().build(authenticationDetailsProvider)
    }

    MonitoringClient createTelemetryClient() {
        String ingestionEndpoint = String.format("https://telemetry-ingestion.%s.oraclecloud.com",
                regionProvider.getRegion().getRegionId());
        return MonitoringClient.builder().endpoint(ingestionEndpoint).build(authenticationDetailsProvider)
    }

    PostMetricDataDetails createMetricData(String name, List<Datapoint> datapoints) {
        return PostMetricDataDetails.builder()
                .metricData([
                        MetricDataDetails.builder()
                                .name(name)
                                .compartmentId(compartmentId)
                                .namespace("micronaut_test")
                                .datapoints(datapoints)
                                .dimensions(["host": "some-host"])
                                .build()
                ])
                .build()
    }

}
