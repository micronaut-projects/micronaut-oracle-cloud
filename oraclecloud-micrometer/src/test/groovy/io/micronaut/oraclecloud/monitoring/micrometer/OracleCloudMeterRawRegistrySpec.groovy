package io.micronaut.oraclecloud.monitoring.micrometer

import com.oracle.bmc.monitoring.MonitoringClient
import com.oracle.bmc.monitoring.model.Datapoint
import io.micrometer.core.instrument.*
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.util.stream.Collectors

class OracleCloudMeterRawRegistrySpec extends Specification {

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
    private MonitoringClient monitoringClient = Mock(MonitoringClient)

    @AutoCleanup
    @Shared
    private OracleCloudRawMeterRegistry cloudMeterRegistry = new OracleCloudRawMeterRegistry(oracleCloudConfig, mockClock, monitoringClient)

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
        def details =  cloudMeterRegistry.metricDataDetails(id, null, List.of(Datapoint.builder().value(1d).timestamp(new Date()).build()))

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
        data.first().name == "gauge"
        data.first().datapoints.first().value == 1d
    }

    def "test it can track counter"() {
        given:
        def counter = cloudMeterRegistry.counter("counter")
        counter.increment()
        counter.increment()

        when:
        mockClock.add(oracleCloudConfig.step())
        def data = cloudMeterRegistry.trackRawData(counter).findFirst().get()

        then:
        data.name == "counter"
        data.datapoints
        data.datapoints.size() == 2
        data.datapoints[0].value == 1
        data.datapoints[1].value == 1
    }

    def "test it can track timer"() {
        given:
        def timer = cloudMeterRegistry.timer("timer")
        timer.record(Duration.ofSeconds(1))
        timer.record(Duration.ofSeconds(2))

        when:
        mockClock.add(oracleCloudConfig.step())
        def data = cloudMeterRegistry.trackRawData(timer).collect(Collectors.toList())

        then:
        data.size() == 1
        data[0].name == "timer"
        data[0].datapoints.size() == 2
        data[0].datapoints[0].value == 1000
        data[0].datapoints[1].value == 2000
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
        def data = cloudMeterRegistry.trackRawData(summary).collect(Collectors.toList())


        then:
        data.size() == 1
        data[0].name == "distributionSummary"
        data[0].datapoints.size() == 3
        data[0].datapoints[0].value == 10 * 100
        data[0].datapoints[1].value == 12 * 100
        data[0].datapoints[2].value == 14 * 100
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
        data[0].name == "functionCounter"
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
        data.size() == 1
        data[0].name == "functionTimer"
        data[0].datapoints.first().value == 1
    }

    def "test it can track long last timer"() {
        given:
        def longTaskTimer = LongTaskTimer.builder("LongTaskTimer").register(cloudMeterRegistry);

        mockClock.add(oracleCloudConfig.step());

        when:
        def data = cloudMeterRegistry.trackLongTaskTimer(longTaskTimer).collect(Collectors.toList())

        then:
        data
        data.size() == 2
        data[0].name == "LongTaskTimer"
        data[1].name == "LongTaskTimer_activeTasks"
    }
}
