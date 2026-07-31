package io.micronaut.oraclecloud.monitoring.sdk

import com.oracle.bmc.Service
import com.oracle.bmc.Services
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider
import com.oracle.bmc.common.RegionalClientBuilder
import com.oracle.bmc.http.client.HttpProvider
import com.oracle.bmc.http.client.Method
import com.oracle.bmc.http.internal.BaseSyncClient
import com.oracle.bmc.http.signing.SigningStrategy
import com.oracle.bmc.http.signing.internal.DefaultRequestSignerFactory
import com.oracle.bmc.monitoring.model.Alarm
import com.oracle.bmc.monitoring.requests.GetAlarmRequest
import com.oracle.bmc.monitoring.responses.GetAlarmResponse
import com.oracle.bmc.util.CircuitBreakerUtils
import com.oracle.bmc.util.internal.Validate
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.annotation.Nonnull
import jakarta.inject.Singleton;
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

@MicronautTest(startApplication = false)
@Property(name = "micronaut.metrics.enabled", value = "false")
class OracleCloudSdkMetricsFilterCustomClientSpec extends Specification {

    def "test oci sdk metrics client filter request returns 200"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.enabled": "true",
                "micronaut.metrics.export.oraclecloud.enabled": "false",
                "spec.name": "OracleCloudSdkMetricsFilterCustomClientSpec"
        ])

        EmbeddedServer embeddedServer = context.getBean(EmbeddedServer)
        embeddedServer.start()

        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class)

        CustomMetricsClient customMetricsClient = context.getBean(CustomMetricsClient.class)
        customMetricsClient.setEndpoint(embeddedServer.getURL().toString())
        customMetricsClient.getAlarm(GetAlarmRequest.builder().alarmId("test").build())

        expect:
        def bean = context.getBean(SdkMetricsNettyClientFilter)
        bean
        def meter = meterRegistry.getMeters().find(x -> x.getId().toString().contains("oci.sdk.client"))
        meter
        meter.id.getTag("host") == "localhost"
        meter.id.getTag("http_method") == "GET"
        meter.id.getTag("status") == "200"
        meter.id.getTag("exception") == "none"
        meter.id.getTag("class_and_method") == "OracleCloudSdkMetricsFilterCustomClientSpec\$CustomMetricsClient.getAlarm"

        cleanup:
        embeddedServer.stop()
    }

    @Controller('/20180401/alarms')
    @Requires(property = "spec.name", value = "OracleCloudSdkMetricsFilterCustomClientSpec")
    static class WordsController {

        @Get("/test")
        String test() {
           return "{\"status\":\"OK\"}"
        }

        @Get("/test2")
        String test2() {
            return "{\"status\":\"OK\"}"
        }
    }

    @Singleton
    @Requires(property = "spec.name", value = "OracleCloudSdkMetricsFilterCustomClientSpec")
    static class CustomMetricsClient extends BaseSyncClient {

        private static final Logger LOG = LoggerFactory.getLogger(CustomMetricsClient.class)

        protected CustomMetricsClient(CustomMetricsClientBuilder builder,
                                      AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
            super(
                    builder,
                    authenticationDetailsProvider,
                    CircuitBreakerUtils.DEFAULT_CIRCUIT_BREAKER_CONFIGURATION)
        }

        GetAlarmResponse getAlarm(GetAlarmRequest request) {

            Validate.notBlank(request.getAlarmId(), "alarmId must not be blank");

            return clientCall(request, GetAlarmResponse::builder)
                    .logger(LOG, "getAlarm")
                    .serviceDetails(
                            "Monitoring",
                            "GetAlarm",
                            "https://docs.oracle.com/iaas/api/#/en/monitoring/20180401/Alarm/GetAlarm")
                    .method(Method.GET)
                    .requestBuilder(GetAlarmRequest::builder)
                    .basePath("/20180401")
                    .appendPathParam("alarms")
                    .appendPathParam(request.getAlarmId())
                    .accept("application/json")
                    .appendHeader("opc-request-id", request.getOpcRequestId())
                    .handleBody(
                            Alarm.class,
                            GetAlarmResponse.Builder::alarm)
                    .handleResponseHeaderString("etag", GetAlarmResponse.Builder::etag)
                    .handleResponseHeaderString(
                            "opc-request-id", GetAlarmResponse.Builder::opcRequestId)
                    .callSync();
        }
    }


     @Singleton
     @Requires(property = "spec.name", value = "OracleCloudSdkMetricsFilterCustomClientSpec")
     static class CustomMetricsClientBuilder extends RegionalClientBuilder<CustomMetricsClientBuilder, CustomMetricsClient> {

         public static final Service SERVICE =
                 Services.serviceBuilder()
                         .serviceName("MONITORING")
                         .serviceEndpointPrefix("telemetry")
                         .serviceEndpointTemplate("https://telemetry.{region}.{secondLevelDomain}")
                         .build()

         CustomMetricsClientBuilder(@Nullable HttpProvider httpProvider) {
             super(SERVICE)
             this.requestSignerFactory = new DefaultRequestSignerFactory(SigningStrategy.STANDARD)
             this.httpProvider(httpProvider)
         }

         CustomMetricsClient build(@Nonnull AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
             return new CustomMetricsClient(this, authenticationDetailsProvider)
         }
    }

}
