package io.micronaut.oraclecloud.logging

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.context.env.Environment
import io.micronaut.core.io.Readable
import jakarta.inject.Singleton
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

        cleanup:
        context.close()
    }

    def "readable property injection works with oracle logging on the classpath"() {
        given:
        new OracleCloudJsonFormatter().toJsonString([message: "starting"])
        ApplicationContext context = ApplicationContext.run([
                "spec.name"     : "OracleCloudLoggingClientSpec",
                "app.filepath"  : "classpath:data/addresses.csv",
                "oci.logging.enabled": "true",
        ], Environment.ORACLE_CLOUD)

        when:
        ReadableConfiguration configuration = context.getBean(ReadableConfiguration)

        then:
        configuration.readable.exists()
        configuration.readable.name.endsWith("addresses.csv")

        cleanup:
        context.close()
    }

    @Requires(property = "spec.name", value = "OracleCloudLoggingClientSpec")
    @Singleton
    static class ReadableConfiguration {
        final Readable readable

        ReadableConfiguration(@Value('${app.filepath}') Readable readable) {
            this.readable = readable
        }
    }

}
