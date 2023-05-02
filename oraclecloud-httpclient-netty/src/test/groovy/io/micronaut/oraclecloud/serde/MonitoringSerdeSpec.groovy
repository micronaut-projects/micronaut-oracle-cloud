package io.micronaut.oraclecloud.serde


import com.oracle.bmc.monitoring.model.Alarm
import com.oracle.bmc.monitoring.model.Metric
import io.micronaut.runtime.server.EmbeddedServer

class MonitoringSerdeSpec extends SerdeSpecBase {

    void "Metric serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()
        Metric metric = Metric.builder()
                .compartmentId("a")
                .build()

        when:
        var body = echoTest(embeddedServer, metric)

        then:
        '{"compartmentId":"a"}' == body
    }

    void "Explicitly set Metric serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()
        Metric metric = Metric.builder()
                .compartmentId("a")
                .dimensions(null)
                .build()

        when:
        var body = echoTest(embeddedServer, metric)

        then:
        '{"compartmentId":"a","dimensions":null}' == body
    }

    void "Metric deserialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()
        var body = '{"compartmentId":"a","dimensions":null,"namespace":"name"}'

        when:
        var metric = echoTest(embeddedServer, body, Metric)

        then:
        Metric expected = Metric.builder()
                .compartmentId("a")
                .dimensions(null)
                .namespace("name")
                .build()
        equalsIgnoreExplicitlySet(expected, metric)
    }

    void "Alarm.Severity serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var body = echoTest(embeddedServer, severity)

        then:
        expected == body

        where:
        severity                        | expected
        Alarm.Severity.Critical         | '"CRITICAL"'
        Alarm.Severity.Info             | '"INFO"'
        Alarm.Severity.UnknownEnumValue | "null"
    }

    void "Alarm.Severity deserialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var severity = echoTest(embeddedServer, body, Alarm.Severity)

        then:
        expected == severity

        where:
        body         | expected
        '"CRITICAL"' | Alarm.Severity.Critical
        '"ERROR"'    | Alarm.Severity.Error
        'null'       | Alarm.Severity.UnknownEnumValue
        '"UNKNOWN"'  | Alarm.Severity.UnknownEnumValue
    }

    void "Alarm serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var body = echoTest(embeddedServer, alarm)

        then:
        expected == body

        where:
        alarm   | expected
        Alarm.builder().compartmentId("a").id("1").displayName("name").build()
                | '{"id":"1","displayName":"name","compartmentId":"a"}'
        Alarm.builder().id("1").severity(Alarm.Severity.Error).build()
                | '{"id":"1","severity":"ERROR"}'
        Alarm.builder().id("1").severity(Alarm.Severity.UnknownEnumValue).build()
                | '{"id":"1","severity":null}'
        Alarm.builder().id("1").severity(Alarm.Severity.Critical).query("find all").displayName("name").build()
                | '{"id":"1","displayName":"name","query":"find all","severity":"CRITICAL"}'
    }

    void "Alarm deserialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        def alarm = echoTest(embeddedServer, body, Alarm)

        then:
        equalsIgnoreExplicitlySet(expected, alarm)

        where:
        expected   | body
        Alarm.builder().compartmentId("a").id("1").displayName("name").build()
                | '{"id":"1","displayName":"name","compartmentId":"a"}'
        Alarm.builder().id("1").severity(Alarm.Severity.Error).build()
                | '{"id":"1","severity":"ERROR"}'
        Alarm.builder().id("1").severity(Alarm.Severity.UnknownEnumValue).build()
                | '{"id":"1","severity":null}'
        Alarm.builder().id("1").severity(Alarm.Severity.UnknownEnumValue).build()
                | '{"id":"1","severity":"Invalid Value"}'
        Alarm.builder().id("1").severity(Alarm.Severity.Critical).query("find all").displayName("name").build()
                | '{"id":"1","displayName":"name","query":"find all","severity":"CRITICAL"}'
    }

}
