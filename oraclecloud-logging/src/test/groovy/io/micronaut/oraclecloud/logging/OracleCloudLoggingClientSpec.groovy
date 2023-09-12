package io.micronaut.oraclecloud.logging

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import spock.lang.Specification

@Property(name = "spec.name", value = "OracleCloudLoggingClientSpec")
class OracleCloudLoggingClientSpec extends Specification {

    def "test it not loads when globally disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run([
                "oci.logging.enabled": "false",
        ], Environment.ORACLE_CLOUD)

        expect:
        !context.containsBean(OracleCloudLoggingClient)
    }


}
