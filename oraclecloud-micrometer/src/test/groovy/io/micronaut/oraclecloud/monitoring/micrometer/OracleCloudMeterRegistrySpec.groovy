package io.micronaut.oraclecloud.monitoring.micrometer

import com.oracle.bmc.monitoring.MonitoringClient
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.FunctionTimer
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.Tags
import io.micronaut.oraclecloud.monitoring.MonitoringIngestionClient
import jakarta.inject.Provider
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.util.stream.Collectors

class OracleCloudMeterRegistrySpec extends Specification {

    @Shared
    private MockClock mockClock = new MockClock()

    @Shared
    private OracleCloudConfig oracleCloudConfig = new OracleCloudConfig() {

        @Override
        String compartmentId() {
            return "compartmentId"
        }

        @Override
        String namespace() {
            return "namespace"
        }

        @Override
        String applicationName() {
            return "appName"
        }

        @Override
        String get(String key) {
            return null
        }
    }

    @Shared
    private MonitoringIngestionClient monitoringClient = Mock(MonitoringIngestionClient)

    @AutoCleanup
    @Shared
    private OracleCloudMeterRegistry cloudMeterRegistry = new OracleCloudMeterRegistry(oracleCloudConfig, mockClock, new Provider<MonitoringIngestionClient>() {
        @Override
        MonitoringIngestionClient get() {
            monitoringClient
        }
    })

    def cleanup() {
        cloudMeterRegistry.clear()
    }

    def "test it generates name with suffix"() {
        given:
        Meter.Id id = new Meter.Id("name", Tags.empty(), null, null, Meter.Type.COUNTER)

        expect:
        cloudMeterRegistry.getMetricName(id, null) == "name"
        cloudMeterRegistry.getMetricName(id, "suffix") == "name_suffix"
    }

    def "test it validates dimension keys"() {
        given:
        def tags = Tags.of("valid", "tag").and("", "missing key").and("missing value", "")

        when:
        def dimensions = cloudMeterRegistry.toDimensions(tags.asList())

        then:
        dimensions
        dimensions.size() == 1
        dimensions.containsKey("valid")
    }

    def "test metric with NaN is not added"() {
        given:
        cloudMeterRegistry.gauge("gauge", Double.NaN)

        expect:
        cloudMeterRegistry.metricData.size() == 0
    }

    def "test it adds metric description to metadata"(){
        given:
        Meter.Id id = new Meter.Id("name", Tags.empty(), null, "description in metadata", Meter.Type.COUNTER)

        when:
        def details =  cloudMeterRegistry.metricDataDetails(id, null, 1d)

        then:
        details.metadata
        details.metadata.containsKey("description")
        details.metadata.get("description") == "description in metadata"
    }

    def "test it can track gauge"() {
        given:
        cloudMeterRegistry.gauge("gauge", 1d)

        when:
        def data = cloudMeterRegistry.getMetricData()

        then:
        data
        data.size() == 1
        data.first().datapoints
        data.first().datapoints.size() == 1
        data.first().name == "gauge_value"
        data.first().datapoints.first().value == 1d
    }

    def "test it can track counter"() {
        given:
        def counter = cloudMeterRegistry.counter("counter")
        counter.increment()
        counter.increment()

        when:
        mockClock.add(oracleCloudConfig.step())
        def data = cloudMeterRegistry.trackCounter(counter).findFirst().get()

        then:
        data.name == "counter_count"
        data.datapoints
        data.datapoints.size() == 1
        data.datapoints.first().value == 2
    }

    def "test it can track timer"() {
        given:
        def timer = cloudMeterRegistry.timer("timer")
        timer.record(() -> sleep(500))
        timer.record(() -> sleep(500))

        when:
        mockClock.add(oracleCloudConfig.step())
        def data = cloudMeterRegistry.trackTimer(timer).collect(Collectors.toList())

        then:
        data.size() == 4
        data[0].name == "timer_sum"
        data[1].name == "timer_count"
        data[2].name == "timer_avg"
        data[3].name == "timer_max"
    }

    def "test it can track distribution summary"() {
        given:
        DistributionSummary summary = DistributionSummary
                .builder("distributionSummary")
                .scale(100)
                .register(cloudMeterRegistry);
        summary.record(10)
        summary.record(12)
        summary.record(14)
        mockClock.add(oracleCloudConfig.step())

        when:
        def data = cloudMeterRegistry.trackDistributionSummary(summary).collect(Collectors.toList())

        then:
        data
        data.size() == 4
        data[0].name == "distributionSummary_sum"
        data[0].datapoints.first().value == (10 + 12 + 14) * 100
        data[1].name == "distributionSummary_count"
        data[1].datapoints.first().value == 3
        data[2].name == "distributionSummary_avg"
        data[2].datapoints.first().value == ((10 + 12 + 14) * 100) / 3
        data[3].name == "distributionSummary_max"
        data[3].datapoints.first().value == 1400
    }

    def "test it can track function counter"(){
        given:
        def functionCounter = FunctionCounter.builder("functionCounter", 5d, x -> (double) x)
                .register(cloudMeterRegistry)
        mockClock.add(oracleCloudConfig.step())

        when:
        def data = cloudMeterRegistry.trackFunctionCounter(functionCounter).collect(Collectors.toList())

        then:
        data
        data.size() == 1
        data[0].name == "functionCounter_count"
        data[0].datapoints.first().value == 5
    }

    def "test it can track function timer"() {
        given:
        def functionTimer = FunctionTimer.builder("functionTimer", 1d,
                x -> (long) x,
                x -> (double) x,
                cloudMeterRegistry.getBaseTimeUnit())
                .register(cloudMeterRegistry)
        mockClock.add(oracleCloudConfig.step());

        when:
        def data = cloudMeterRegistry.trackFunctionTimer(functionTimer).collect(Collectors.toList())

        then:
        data
        data.size() == 3
        data[0].name == "functionTimer_count"
        data[0].datapoints.first().value == 1
        data[1].name == "functionTimer_sum"
        data[1].datapoints.first().value == 1
        data[2].name == "functionTimer_avg"
        data[2].datapoints.first().value == 1
    }
}
