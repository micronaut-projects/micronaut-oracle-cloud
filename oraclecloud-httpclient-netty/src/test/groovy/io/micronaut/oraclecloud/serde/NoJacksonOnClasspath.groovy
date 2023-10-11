package io.micronaut.oraclecloud.serde

import spock.lang.Specification

class NoJacksonOnClasspath extends Specification {
    def 'no jackson on classpath'() {
        when:
        Class.forName("com.fasterxml.jackson.databind.ObjectMapper")
        then:
        thrown ClassNotFoundException
    }
}
