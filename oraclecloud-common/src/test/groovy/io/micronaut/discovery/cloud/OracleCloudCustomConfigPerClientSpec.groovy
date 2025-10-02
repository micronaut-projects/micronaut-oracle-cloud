package io.micronaut.discovery.cloud

import com.oracle.bmc.ClientConfiguration
import com.oracle.bmc.retrier.RetryOptions
import com.oracle.bmc.waiter.ExponentialBackoffDelayStrategy
import com.oracle.bmc.waiter.FixedTimeDelayStrategy
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy
import com.oracle.bmc.waiter.MaxTimeTerminationStrategy
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.Specification

class OracleCloudCustomConfigPerClientSpec extends Specification {

    void "test FixedTimeDelayStrategy strategy per client"() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudCustomConfigPerClientSpec",
                        "oci.clients.identity.retry-delay-strategy.max-delay-millis": "100",
                        'micronaut.server.port': -1
                ], Environment.ORACLE_CLOUD) as EmbeddedServer

        def ctx = server.applicationContext
        def config = ctx.getBean(ClientConfiguration, Qualifiers.byName("identity"))

        when:
        def delayStrategy = config.getRetryConfiguration().delayStrategy

        then:
        delayStrategy.getClass() == FixedTimeDelayStrategy.class
        delayStrategy.properties.get("timeBetweenAttempsInMillis") == 100

        cleanup:
        ctx.close()
        server.close()
    }

    void "test ExponentialBackoffDelayStrategy strategy per client"() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudCustomConfigPerClientSpec",
                        "oci.clients.identity.retry-delay-strategy.time-between-attempts-in-millis": "100",
                        "oci.clients.account.read-timeout": "PT2M",
                        'micronaut.server.port': -1
                ], Environment.ORACLE_CLOUD) as EmbeddedServer

        def ctx = server.applicationContext
        def config = ctx.getBean(ClientConfiguration, Qualifiers.byName("identity"))

        when:
        def delayStrategy = config.getRetryConfiguration().delayStrategy

        then:
        delayStrategy.getClass() == ExponentialBackoffDelayStrategy.class
        delayStrategy.properties.get("maxDelayInMillis") == 100

        when:
        def accountConfig = ctx.getBean(ClientConfiguration, Qualifiers.byName("account"))

        then:
        accountConfig
        accountConfig.readTimeoutMillis == 120000

        cleanup:
        ctx.close()
        server.close()
    }

    void "test MaxAttemptsTerminationStrategy strategy per client"() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudCustomConfigPerClientSpec",
                        "oci.clients.identity.retry-termination-strategy.max-attempts": "100",
                        'micronaut.server.port': -1
                ], Environment.ORACLE_CLOUD) as EmbeddedServer

        def ctx = server.applicationContext
        def config = ctx.getBean(ClientConfiguration, Qualifiers.byName("identity"))

        when:
        def terminationStrategy = config.getRetryConfiguration().terminationStrategy

        then:
        terminationStrategy.getClass() == MaxAttemptsTerminationStrategy.class
        terminationStrategy.properties.get("maxAttempts") == 100

        cleanup:
        ctx.close()
        server.close()
    }

    void "test MaxTimeTerminationStrategy strategy per client"() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudCustomConfigPerClientSpec",
                        "oci.clients.identity.retry-termination-strategy.max-time-millis": "100",
                        'micronaut.server.port': -1
                ], Environment.ORACLE_CLOUD) as EmbeddedServer

        def ctx = server.applicationContext
        def config = ctx.getBean(ClientConfiguration, Qualifiers.byName("identity"))

        when:
        def terminationStrategy = config.getRetryConfiguration().terminationStrategy

        then:
        terminationStrategy.getClass() == MaxTimeTerminationStrategy.class
        terminationStrategy.properties.get("maxTimeInMillis") == 100

        cleanup:
        ctx.close()
        server.close()
    }

    void "test retryOptions per client"() {
        given:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudCustomConfigPerClientSpec",
                        "oci.clients.identity.retry-options.mark-read-limit": "100",
                        'micronaut.server.port': -1
                ], Environment.ORACLE_CLOUD) as EmbeddedServer

        def ctx = server.applicationContext
        def config = ctx.getBean(ClientConfiguration, Qualifiers.byName("identity"))

        when:
        def retryOptions = config.getRetryConfiguration().retryOptions

        then:
        retryOptions.getClass() == RetryOptions.class
        retryOptions.markReadLimit == 100

        cleanup:
        ctx.close()
        server.close()
    }

    void "test read timeout per client"() {
        when:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudCustomConfigPerClientSpec",
                        "oci.clients.identity.read-timeout": "100ms",
                        'micronaut.server.port': -1
                ], Environment.ORACLE_CLOUD) as EmbeddedServer

        def ctx = server.applicationContext
        def config = ctx.getBean(ClientConfiguration, Qualifiers.byName("identity"))


        then:
        config.getReadTimeoutMillis() == 100

        cleanup:
        ctx.close()
        server.close()
    }

    void "test read timeout in millis per client"() {
        when:
        EmbeddedServer server = ApplicationContext.run(EmbeddedServer,
                [
                        "spec.name": "OracleCloudCustomConfigPerClientSpec",
                        "oci.clients.identity.read-timeout-millis": "100",
                        'micronaut.server.port': -1
                ], Environment.ORACLE_CLOUD) as EmbeddedServer

        def ctx = server.applicationContext
        def httpConfig = ctx.getBean(HttpClientConfiguration, Qualifiers.byName("identity"))

        then:
        httpConfig.getReadTimeout().get().toMillis() == 100

        cleanup:
        ctx.close()
        server.close()
    }
}
