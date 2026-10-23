package io.micronaut.oraclecloud.monitoring.sdk

import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.oraclecloud.httpclient.netty.OciNettyClientFilter
import spock.lang.Specification

class SdkMetricsNettyClientFilterRequirementsSpec extends Specification {

    void 'SDK metrics filter is disabled when the context has no meter registry'() {
        given:
        ApplicationContext context = ApplicationContext.builder()
            .bootstrapEnvironment(false)
            .eagerBeansEnabled(false)
            .eventsEnabled(false)
            .properties([
                'oci.config.enabled': false,
                'micronaut.metrics.enabled': true,
                'micronaut.metrics.export.oraclecloud.enabled': false
            ])
            .beansPredicate(bean -> !MeterRegistry.isAssignableFrom(bean.beanType))
            .start()

        expect:
        !context.containsBean(MeterRegistry)
        !context.containsBean(SdkMetricsNettyClientFilter)
        !context.getBeansOfType(OciNettyClientFilter).any { it instanceof SdkMetricsNettyClientFilter }

        cleanup:
        context.close()
    }
}
