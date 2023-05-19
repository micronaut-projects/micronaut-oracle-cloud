package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.json.JsonMapper;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

@MicronautTest
@Requires(bean = AuthenticationDetailsProvider.class)
@Property(name = "spec.name", value = "ManagedTest")
public class ManagedTest {
    @Inject
    ApplicationContext ctx;

    @Test
    public void managedClientUsesManagedProvider() {
        MockProvider provider = ctx.getBean(MockProvider.class);
        Assertions.assertEquals(0, provider.buildersCreated);
        ObjectStorageClient client = ctx.getBean(ObjectStorageClient.class);
        client.setRegion(Region.EU_MADRID_1);
        Assertions.assertEquals(1, provider.buildersCreated);
    }

    @Singleton
    @Replaces(ManagedNettyHttpProvider.class)
    @Requires(property = "spec.name", value = "ManagedTest")
    public static class MockProvider extends ManagedNettyHttpProvider {
        int buildersCreated = 0;

        public MockProvider(@Client(id = SERVICE_ID) HttpClient mnHttpClient, @Named(TaskExecutors.BLOCKING) ExecutorService ioExecutor, JsonMapper jsonMapper) {
            super(mnHttpClient, ioExecutor, jsonMapper);
        }

        @Override
        public HttpClientBuilder newBuilder() {
            buildersCreated++;
            return super.newBuilder();
        }
    }
}
