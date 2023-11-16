package io.micronaut.oraclecloud.database;

import com.oracle.bmc.database.DatabaseAsyncClient;
import com.oracle.bmc.database.DatabaseClient;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class DatabaseFactoryTest {

    public static final String ENDPOINT = "http://my_endpoint";
    public static final String ASYNC_ENDPOINT = "http://my_endpoint_async";
    private final DatabaseClient databaseClient;
    private final DatabaseAsyncClient databaseAsyncClient;

    public DatabaseFactoryTest(
            DatabaseClient databaseClient,
            DatabaseAsyncClient databaseAsyncClient
    ) {
        this.databaseClient = databaseClient;
        this.databaseAsyncClient = databaseAsyncClient;
    }

    @Test
    void testDataBaseClientConfiguration() {
        assertEquals(ENDPOINT, databaseClient.getEndpoint());
    }

    @Test
    void testDataBaseAsyncClientConfiguration() {
        assertEquals(ASYNC_ENDPOINT, databaseAsyncClient.getEndpoint());
    }

    @Singleton
    static class DatabaseClientBuilderListener
            implements BeanCreatedEventListener<DatabaseClient.Builder> {
        @Override
        public DatabaseClient.Builder onCreated(
            @NonNull BeanCreatedEvent<DatabaseClient.Builder> event
        ) {
            DatabaseClient.Builder builder = event.getBean();
            builder.endpoint(DatabaseFactoryTest.ENDPOINT);
            return builder;
        }
    }

    @Singleton
    static class DatabaseAsyncClientBuilderListener
            implements BeanCreatedEventListener<DatabaseAsyncClient.Builder> {
        @Override
        public DatabaseAsyncClient.Builder onCreated(
            @NonNull BeanCreatedEvent<DatabaseAsyncClient.Builder> event
        ) {
            DatabaseAsyncClient.Builder builder = event.getBean();
            builder.endpoint(DatabaseFactoryTest.ASYNC_ENDPOINT);
            return builder;
        }
    }

}
