package io.micronaut.oraclecloud.monitoring.micrometer

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import jakarta.inject.Inject
import spock.lang.Specification

class OracleCloudNamespaceValidationSpec extends Specification {

    @Inject
    ApplicationContext context

    def "test valid namespaces"() {
        given:
        var pattern = OracleCloudConfig.NAMESPACE_PATTERN

        expect:
        pattern.matcher(string).matches()

        where:
        string | _
        "namespace" | _
        "namespace123" | _
        "my_space_123" | _
        "example_namespace" | _
    }

    def "test invalid namespaces"() {
        given:
        var pattern = OracleCloudConfig.NAMESPACE_PATTERN

        expect:
        !pattern.matcher(string).matches()

        where:
        string | _
        "one&two" | _
        "-namespace" | _
        "Namespace%Example" | _
        "123-my-space" | _
        "NAMESPACE" | _
        "example-namespace" | _
        "EXAMPLE-Namespace-example123" | _
        "my-space-123" | _
    }

    def "test valid namespace"() {
        when:
        var config = new ConfigurableOracleCloudConfig("micronaut_test_ns_1")

        then:
        config.validate().failures().empty
    }

    def "test namespace starting with oci_ is invalid"() {
        when:
        var config = new ConfigurableOracleCloudConfig("oci_metrics_demo")

        then:
        config.validate().failures().size() > 0
    }

    def "test namespace with invalid symbols is not validated"() {
        when:
        var config = new ConfigurableOracleCloudConfig("METRICS_demo")

        then:
        config.validate().failures().size() > 0

        when:
        config = new ConfigurableOracleCloudConfig("my_demo_%")

        then:
        config.validate().failures().size() > 0

        when:
        config = new ConfigurableOracleCloudConfig("my-demo")

        then:
        config.validate().failures().size() > 0
    }

    class ConfigurableOracleCloudConfig implements OracleCloudConfig {
        String namespace

        ConfigurableOracleCloudConfig(String namespace) {
            this.namespace = namespace
        }

        @Override
        String get(String key) {
            if (key == 'oraclecloud.namespace') {
                return namespace
            } else if (key == 'oraclecloud.enabled') {
                return true
            }
            return null
        }
    }
}
