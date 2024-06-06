package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.Region;
import com.oracle.bmc.Service;
import com.oracle.bmc.Services;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.common.ClientBuilderBase;
import com.oracle.bmc.http.internal.BaseSyncClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.client.HttpClientRegistry;
import io.micronaut.http.client.HttpVersionSelection;
import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.client.netty.DefaultNettyHttpClientRegistry;
import io.micronaut.inject.InjectionPoint;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
@Property(name = "spec.name", value = "ManagedPropertyTest")
public class ManagedPropertyTest {

    public static final Service SERVICE = Services.serviceBuilder()
        .serviceName("unmanaged")
        .serviceEndpointPrefix("")
        .serviceEndpointTemplate("https://unmanaged.{region}.{secondLevelDomain}")
        .build();

    @Inject
    ApplicationContext ctx;

    @Inject
    AbstractAuthenticationDetailsProvider authenticationDetailsProvider;

    @Inject
    ManagedNettyHttpProvider managedNettyHttpProvider;

    @Test
    public void unmanagedClientUsesManagedProviderProperty() {
        MockHttpClientRegistry registry = ctx.getBean(MockHttpClientRegistry.class);
        Assertions.assertFalse(registry.clientRegistered);
        UnmanagedClientBuilder unmanagedClientBuilder = new UnmanagedClientBuilder(SERVICE);
        unmanagedClientBuilder.clientConfigurator(builder -> builder.property(NettyClientProperties.MANAGED_PROVIDER, managedNettyHttpProvider));
        UnmanagedClient unmanagedClient = unmanagedClientBuilder.build(authenticationDetailsProvider);
        unmanagedClient.setRegion(Region.EU_MADRID_1);
        Assertions.assertTrue(registry.clientRegistered);
    }

    @Singleton
    @Replaces(DefaultNettyHttpClientRegistry.class)
    @Requires(property = "spec.name", value = "ManagedPropertyTest")
    static class MockHttpClientRegistry implements HttpClientRegistry<HttpClient> {

        boolean clientRegistered = false;

        @Override
        public @NonNull HttpClient getClient(@NonNull AnnotationMetadata annotationMetadata) {
            return new DefaultHttpClient();
        }

        @Override
        public @NonNull HttpClient getClient(@NonNull HttpVersionSelection httpVersion, @NonNull String clientId, @Nullable String path) {
            clientRegistered = true;
            return new DefaultHttpClient();
        }

        @Override
        public @NonNull HttpClient resolveClient(@Nullable InjectionPoint<?> injectionPoint, @Nullable LoadBalancer loadBalancer, @Nullable HttpClientConfiguration configuration, @NonNull BeanContext beanContext) {
            return new DefaultHttpClient();
        }

        @Override
        public void disposeClient(AnnotationMetadata annotationMetadata) {
        }
    }

    static class UnmanagedClient extends BaseSyncClient {

        protected UnmanagedClient(ClientBuilderBase<?, ?> builder, AbstractAuthenticationDetailsProvider authenticationDetailsProvider, CircuitBreakerConfiguration defaultCircuitBreaker) {
            super(builder, authenticationDetailsProvider, defaultCircuitBreaker);
        }

        @Override
        public void setRegion(Region region) {
            super.setRegion(region);
        }
    }

    static class UnmanagedClientBuilder extends ClientBuilderBase<UnmanagedClientBuilder, UnmanagedClient> {

        public UnmanagedClientBuilder(Service service) {
            super(service);
        }

        @Override
        public UnmanagedClient build(AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
            return new UnmanagedClient(this, authenticationDetailsProvider, null);
        }
    }
}
