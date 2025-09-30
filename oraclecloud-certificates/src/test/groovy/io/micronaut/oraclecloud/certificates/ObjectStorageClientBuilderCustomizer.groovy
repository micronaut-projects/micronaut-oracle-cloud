package io.micronaut.oraclecloud.certificates

import com.oracle.bmc.objectstorage.ObjectStorageClient
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton

@Singleton
class ObjectStorageClientBuilderCustomizer implements BeanCreatedEventListener<ObjectStorageClient.Builder> {

    private final String url

    ObjectStorageClientBuilderCustomizer(EmbeddedServer embeddedServer) {
        this.url = embeddedServer.getURL()
    }

    @Override
    ObjectStorageClient.Builder onCreated(BeanCreatedEvent<ObjectStorageClient.Builder> event) {
        def builder = event.bean
        builder.endpoint(url)
        return builder
    }
}
