package io.micronaut.oraclecloud.serde

import com.fasterxml.jackson.annotation.JsonFilter
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel
import com.oracle.bmc.monitoring.model.Metric
import io.micronaut.oraclecloud.serde.model.BaseModel
import io.micronaut.oraclecloud.serde.model.ComplexModel
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.Unroll

class ExplicitlySetSerdeSpec extends SerdeSpecBase {

    void "OCI SDK Model serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()
        MyModel myModel = new MyModel("value")

        when:
        var body = echoTest(embeddedServer, myModel)

        then:
        '{"string":"value"}' == body
    }

    void "Explicitly set OCI SDK Model serialization test"() throws Exception {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var body = echoTest(embeddedServer, new MyModel(null))

        then:
        '{"string":null}' == body

        when:
        body = echoTest(embeddedServer, new MyModel())

        then:
        '{}' == body
    }

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

    void "Test extra properties deserialization does not throw exception"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var value = '{"string":"value","extraString":"extraValue"}'
        var myModel = echoTest(embeddedServer, value, MyModel)

        then:
        notThrown(Exception)
        myModel.string == "value"
    }

    @Unroll
    void "Complex model serialization test #modelBuilder"() {
        given:
        EmbeddedServer embeddedServer = initContext()

        when:
        var response = echoTest(embeddedServer, modelBuilder.build())

        then:
        response == json

        where:
        modelBuilder                                                                                    | json
        ComplexModel.builder()                                                                          | '{"type":"complex"}'
        ComplexModel.builder().string("one")                                                            | '{"type":"complex","string":"one"}'
        ComplexModel.builder().string(null)                                                             | '{"type":"complex","string":null}'
        ComplexModel.builder().baseString("one").string("two").integer(null)                            | '{"type":"complex","string":"two","int":null,"baseString":"one"}'
        ComplexModel.builder().baseInteger(null).integer(null).list(null)                               | '{"type":"complex","int":null,"list":null,"baseInt":null}'
        ComplexModel.builder().string("one").baseInteger(1).integer(null).baseString(null).list(["1"])  | '{"type":"complex","string":"one","int":null,"list":["1"],"baseString":null,"baseInt":1}'
        ComplexModel.builder().baseString(null).baseInteger(null).string(null).integer(null).list(null) | '{"type":"complex","string":null,"int":null,"list":null,"baseString":null,"baseInt":null}'
    }

    @Unroll
    void "Simple model deserialization for #modelBuilder"() {
        EmbeddedServer embeddedServer = initContext()

        when:
        var response = echoTest(embeddedServer, json, BaseModel)

        then:
        modelBuilder.build().equals(response, false)

        where:
        modelBuilder                                            | json
        BaseModel.builder()                                     | '{}'
        BaseModel.builder().baseString("one").baseInteger(1)    | '{"baseString":"one","baseInt":1}'
        BaseModel.builder().baseString("one")                   | '{"baseString":"one"}'
        BaseModel.builder().baseInteger(null)                   | '{"baseInt":null}'
        BaseModel.builder().baseString("two").baseInteger(null) | '{"baseString":"two","baseInt":null}'
        BaseModel.builder().baseInteger(null).baseString(null)  | '{"baseString":null,"baseInt":null}'
        BaseModel.builder()                                     | '{"type":"unknown"}'
    }

    @Unroll
    void "Complex model deserialization for #modelBuilder"() {
        EmbeddedServer embeddedServer = initContext()

        when:
        var response = echoTest(embeddedServer, json, BaseModel)

        then:
        modelBuilder.build().equals(response, false)

        where:
        modelBuilder                                                                                    | json
        ComplexModel.builder()                                                                          | '{"type":"complex"}'
        ComplexModel.builder().string("one")                                                            | '{"type":"complex","string":"one"}'
        ComplexModel.builder().string(null)                                                             | '{"type":"complex","string":null}'
        ComplexModel.builder().baseString("one").string("two").integer(null)                            | '{"type":"complex","string":"two","int":null,"baseString":"one"}'
        ComplexModel.builder().baseInteger(null).integer(null).list(null)                               | '{"type":"complex","int":null,"list":null,"baseInt":null}'
        ComplexModel.builder().string("one").baseInteger(1).integer(null).baseString(null).list(["1"])  | '{"type":"complex","string":"one","int":null,"list":["1"],"baseString":null,"baseInt":1}'
        ComplexModel.builder().baseString(null).baseInteger(null).string(null).integer(null).list(null) | '{"type":"complex","string":null,"int":null,"list":null,"baseString":null,"baseInt":null}'
    }

    @JsonFilter("explicitlySetFilter")
    static class MyModel extends ExplicitlySetBmcModel {
        String string

        MyModel() {}

        MyModel(String string) {
            this.string = string
            this.markPropertyAsExplicitlySet("string")
        }
    }

}
