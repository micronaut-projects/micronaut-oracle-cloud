package io.micronaut.oraclecloud.factory;

import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.ObjectStorageClient
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.core.annotation.NonNull
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification


@MicronautTest
class ObjectStorageFactorySpec extends Specification {

    public static final String ENDPOINT = "http://my_endpoint"
    public static final String ASYNC_ENDPOINT = "http://my_endpoint_async"

    @Inject
    ObjectStorageClient client

    @Inject
    ObjectStorageAsyncClient asyncClient

    void "test database client configuration"() {
        when:
        client

        then:
        client != null
        client.endpoint == ENDPOINT
    }

    void "test database async client configuration"() {
        when:
        asyncClient

        then:
        asyncClient != null
        asyncClient.endpoint == ASYNC_ENDPOINT
    }

    @Singleton
    static class DatabaseClientBuilderListener
            implements BeanCreatedEventListener<ObjectStorageClient.Builder> {
        @Override
        ObjectStorageClient.Builder onCreated(
            @NonNull BeanCreatedEvent<ObjectStorageClient.Builder> event
        ) {
            var builder = event.bean
            builder.endpoint(ENDPOINT)
            return builder
        }
    }

    @Singleton
    static class DatabaseAsyncClientBuilderListener
            implements BeanCreatedEventListener<ObjectStorageAsyncClient.Builder> {
        @Override
        ObjectStorageAsyncClient.Builder onCreated(
            @NonNull BeanCreatedEvent<ObjectStorageAsyncClient.Builder> event
        ) {
            var builder = event.bean
            builder.endpoint(ASYNC_ENDPOINT)
            return builder
        }
    }

}
