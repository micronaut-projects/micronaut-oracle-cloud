package io.micronaut.oraclecloud.monitoring.primitives

import spock.lang.Specification

class DataPointProviderSpec extends Specification {

    def "uses the default buffer size"() {
        expect:
        new DataPointProvider().produceDatapoints().empty
    }
}
