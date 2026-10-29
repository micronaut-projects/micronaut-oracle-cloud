package io.micronaut.oraclecloud.monitoring.sdk

import com.oracle.bmc.http.client.HttpRequest
import com.oracle.bmc.http.client.HttpResponse
import com.oracle.bmc.http.client.Method
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires
import io.micronaut.jdbc.metadata.DataSourcePoolMetadata
import spock.lang.Specification

import javax.sql.DataSource

import static io.micronaut.oraclecloud.httpclient.netty.NettyClientProperties.CLASS_AND_METHOD_KEY_NAME

class OracleCloudSdkMetricsDataSourceCycleSpec extends Specification {

    def "SDK metrics during datasource initialization do not recursively resolve the meter registry"() {
        given:
        HttpRequest request = Stub() {
            uri() >> URI.create("https://localhost/resource")
            method() >> Method.GET
            attribute(CLASS_AND_METHOD_KEY_NAME) >> "TestClient.getResource"
        }
        HttpResponse response = Stub() {
            status() >> 200
        }
        ApplicationContext context = ApplicationContext.builder().properties([
                "micronaut.metrics.enabled": "true",
                "micronaut.metrics.export.oraclecloud.enabled": "false",
                "spec.data-sources.default.enabled": "true",
                "spec.name": "OracleCloudSdkMetricsDataSourceCycleSpec"
        ]).build()
        context.registerSingleton(HttpRequest, request)
        context.registerSingleton(HttpResponse, response)

        when:
        context.start()
        MeterRegistry meterRegistry = context.getBean(MeterRegistry)

        then:
        context.getBean(OciClientDataSourcePoolMetadata).bootstrapRequestCompleted
        meterRegistry.find("oci.sdk.client").timer() == null

        when:
        SdkMetricsNettyClientFilter metricsFilter = context.getBean(SdkMetricsNettyClientFilter)
        def sample = metricsFilter.beforeRequest(request)
        HttpResponse result = metricsFilter.afterResponse(request, response, null, sample)

        then:
        result.is(response)
        meterRegistry.get("oci.sdk.client").tag("class_and_method", "TestClient.getResource").timer().count() == 1

        cleanup:
        context?.close()
    }

    @EachProperty("spec.data-sources")
    @Requires(property = "spec.name", value = "OracleCloudSdkMetricsDataSourceCycleSpec")
    static class OciClientDataSourcePoolMetadata implements DataSourcePoolMetadata<DataSource> {

        private final SdkMetricsNettyClientFilter metricsFilter
        private final HttpRequest request
        private final HttpResponse response
        boolean bootstrapRequestCompleted

        OciClientDataSourcePoolMetadata(@Parameter String name,
                                       SdkMetricsNettyClientFilter metricsFilter,
                                       HttpRequest request,
                                       HttpResponse response) {
            this.metricsFilter = metricsFilter
            this.request = request
            this.response = response
        }

        @Override
        DataSource getDataSource() {
            def sample = metricsFilter.beforeRequest(request)
            bootstrapRequestCompleted = metricsFilter.afterResponse(request, response, null, sample).is(response)
            return null
        }

        @Override
        Integer getActive() {
            return null
        }

        @Override
        Integer getIdle() {
            return null
        }

        @Override
        Integer getMax() {
            return null
        }

        @Override
        Integer getMin() {
            return null
        }

        @Override
        Float getUsage() {
            return null
        }

        @Override
        String getValidationQuery() {
            return null
        }

        @Override
        Boolean getDefaultAutoCommit() {
            return null
        }
    }
}
