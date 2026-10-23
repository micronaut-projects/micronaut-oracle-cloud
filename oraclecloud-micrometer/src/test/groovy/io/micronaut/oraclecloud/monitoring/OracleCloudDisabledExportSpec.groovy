package io.micronaut.oraclecloud.monitoring

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class OracleCloudDisabledExportSpec extends Specification {

    void 'closing a context with disabled OCI export needs no OCI authentication'() {
        given:
        ApplicationContext context = ApplicationContext.builder()
            .bootstrapEnvironment(false)
            .enableDefaultPropertySources(false)
            .properties([
                'micronaut.metrics.enabled': metricsEnabled,
                'micronaut.metrics.export.oraclecloud.enabled': false,
                'oci.config.enabled': false
            ])
            .start()

        when:
        context.close()

        then:
        noExceptionThrown()

        where:
        metricsEnabled << [true, false]
    }
}
