package io.micronaut.oraclecloud.serde

import spock.lang.Specification

class NoJacksonOnClasspath extends Specification {
    def 'no jackson on classpath'() {
        when:
        Class.forName("tools.jackson.databind.ObjectMapper")
        then:
        thrown ClassNotFoundException
    }
}
