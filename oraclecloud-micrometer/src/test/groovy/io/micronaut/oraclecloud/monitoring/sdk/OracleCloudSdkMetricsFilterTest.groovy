package io.micronaut.oraclecloud.monitoring.sdk

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.monitoring.Monitoring
import com.oracle.bmc.monitoring.requests.GetAlarmRequest
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification


@MicronautTest(startApplication = false)
class OracleCloudSdkMetricsFilterTest extends Specification {

    def "test oci sdk metrics client filter request returns 200"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.enabled": "true",
                "micronaut.metrics.export.oraclecloud.enabled": "false"
        ])

        EmbeddedServer embeddedServer = context.getBean(EmbeddedServer)
        embeddedServer.start()

        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class)

        Monitoring mon = context.getBean(Monitoring.class)
        mon.setEndpoint(embeddedServer.getURL().toString())
        mon.getAlarm(GetAlarmRequest.builder().alarmId("test").build())

        expect:
        context.containsBean(SdkMetricsNettyClientFilter)
        def meter = meterRegistry.getMeters().find(x -> x.getId().toString().contains("oci.sdk.client"))
        meter.id.getTag("host") == "localhost"
        meter.id.getTag("method") == "GET"
        meter.id.getTag("status") == "200"
        meter.id.getTag("exception") == "none"

        cleanup:
        embeddedServer.stop()
    }

    def "test oci sdk metrics client filter disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.enabled": "true",
                "micronaut.metrics.export.oraclecloud.enabled": "false",
                "micronaut.metrics.oci.sdk.client.enabled": "false",
        ])

        EmbeddedServer embeddedServer = context.getBean(EmbeddedServer)
        embeddedServer.start()

        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class)

        Monitoring mon = context.getBean(Monitoring.class)
        mon.setEndpoint(embeddedServer.getURL().toString())
        mon.getAlarm(GetAlarmRequest.builder().alarmId("test2").build())

        expect:
        !context.containsBean(SdkMetricsNettyClientFilter)
        meterRegistry.getMeters().find(x -> x.getId().toString().contains("oci.sdk.client")) == null

        cleanup:
        embeddedServer.stop()
    }

    def "test oci sdk metrics client filter request returns 404" () {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.enabled": "true",
                "micronaut.metrics.export.oraclecloud.enabled": "false"
        ])

        EmbeddedServer embeddedServer = context.getBean(EmbeddedServer)
        embeddedServer.start()

        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class)

        Monitoring mon = context.getBean(Monitoring.class)
        mon.setEndpoint(embeddedServer.getURL().toString())

        when:
        mon.getAlarm(GetAlarmRequest.builder().alarmId("not_exists").build())

        then:
        final BmcException exception = thrown()

        expect:
        exception.message.contains("Error returned by GetAlarm operation in Monitoring service")
        context.containsBean(SdkMetricsNettyClientFilter)
        def meter = meterRegistry.getMeters().find(x -> x.getId().toString().contains("oci.sdk.client"))
        meter.id.getTag("host") == "localhost"
        meter.id.getTag("method") == "GET"
        meter.id.getTag("status") == "404"
        meter.id.getTag("exception") == "none"

        cleanup:
        embeddedServer.stop()
    }

    def "test oci sdk metrics client filter request returns exception" () {
        given:
        ApplicationContext context = ApplicationContext.run([
                "micronaut.metrics.enabled": "true",
                "micronaut.metrics.export.oraclecloud.enabled": "false"
        ])

        EmbeddedServer embeddedServer = context.getBean(EmbeddedServer)
        embeddedServer.start()

        MeterRegistry meterRegistry = context.getBean(MeterRegistry.class)

        Monitoring mon = context.getBean(Monitoring.class)
        mon.setEndpoint("https://localhost:1111")

        when:
        mon.getAlarm(GetAlarmRequest.builder().alarmId("not_exists").build())

        then:
        final BmcException exception = thrown()

        expect:
        exception.message.contains("Error returned by GetAlarm operation in Monitoring service")
        context.containsBean(SdkMetricsNettyClientFilter)
        def meter = meterRegistry.getMeters().find(x -> x.getId().toString().contains("oci.sdk.client"))
        meter.id.getTag("host") == "localhost"
        meter.id.getTag("method") == "GET"
        meter.id.getTag("exception") == "HttpClientException"

        cleanup:
        embeddedServer.stop()
    }

    @Controller('/20180401/alarms')
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

}
