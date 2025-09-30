package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.certificates.CertificatesClient
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

@Singleton
class CertificatesClientBuilderCustomizer implements BeanCreatedEventListener<CertificatesClient.Builder> {

    private final String url

    CertificatesClientBuilderCustomizer(@Value('${test.client.url}')  String url) {
        this.url = url
    }

    @Override
    CertificatesClient.Builder onCreated(BeanCreatedEvent<CertificatesClient.Builder> event) {
        def builder = event.bean
        builder.endpoint(url)
        return builder
    }
}
