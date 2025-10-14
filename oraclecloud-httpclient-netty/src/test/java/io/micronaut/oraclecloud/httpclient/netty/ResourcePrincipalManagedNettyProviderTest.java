package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ProvidesCustomRequestSigner;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.auth.internal.AbstractFederationClient;
import com.oracle.bmc.auth.internal.SecurityTokenAdapter;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.Priorities;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.StandardClientProperties;
import com.oracle.bmc.http.internal.AuthnClientFilter;
import com.oracle.bmc.http.internal.ClientIdFilter;
import com.oracle.bmc.http.internal.LogHeadersFilter;
import com.oracle.bmc.http.signing.RequestSigner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

/**
 * Verifies that ResourcePrincipalAuthenticationDetailsProvider is created by Micronaut and,
 * during its internal federation client creation, the Netty HttpProvider in use is the managed
 * ManagedNettyHttpProvider (wired automatically by Micronaut), such that managed-only properties
 * like SERVICE_ID are accepted without throwing.
 *
 * This test does NOT manually fetch HttpProvider or assert SPI defaults. It relies on Micronaut
 * to initialize ManagedNettyHttpProvider before constructing the RP provider bean.
 */
public class ResourcePrincipalManagedNettyProviderTest {

    @Test
    void makeClientUsesManagedNettyHttpProvider() {
        // Run the application context so Micronaut wires ManagedNettyHttpProvider first (@Context)
        try (ApplicationContext ctx = ApplicationContext.builder()
            .properties(java.util.Map.of("spec.name", "ResourcePrincipalManagedNettyProviderTest"))
            .start()) {
            ResourcePrincipalAuthenticationDetailsProvider rp = ctx.getBean(ResourcePrincipalAuthenticationDetailsProvider.class);
            RecordingFederationClient federationClient = ctx.getBean(RecordingFederationClient.class);

            // Assert that makeClient() was invoked and returned a Netty client instance
            Assertions.assertNotNull(federationClient.lastClient, "Expected AbstractFederationClient.makeClient to be invoked during construction");
            String actualClientClass = federationClient.lastClient.getClass().getName();
            Assertions.assertTrue(actualClientClass.contains("io.micronaut.oraclecloud.httpclient.netty.NettyHttpClient"),
                "Expected NettyHttpClient implementation, but was: " + actualClientClass);

            NettyHttpClient client = (NettyHttpClient) federationClient.lastClient;

            Assertions.assertFalse(client.nettyClientFilter.isEmpty());

                // Also ensure bean was created
            Assertions.assertNotNull(rp, "Expected ResourcePrincipalAuthenticationDetailsProvider bean");
        }
    }

    @Factory
    @Requires(property = "spec.name", value = "ResourcePrincipalManagedNettyProviderTest")
    static class TestFactory {
        @Singleton
        TestSessionKeySupplier sessionKeySupplier() {
            return new TestSessionKeySupplier();
        }

        @Singleton
        DummyBasicProvider basicProvider() {
            return new DummyBasicProvider();
        }

        @Singleton
        RecordingFederationClient federationClient(TestSessionKeySupplier sks, DummyBasicProvider basic) {
            return new RecordingFederationClient(
                "http://127.0.0.1:1/rpt", // dummy, no real call is made during construction
                "http://127.0.0.1:1/rpst",
                sks,
                basic,
                null,
                null
            );
        }

        @Singleton
        ResourcePrincipalAuthenticationDetailsProvider rpProvider(RecordingFederationClient federationClient,
                                                                 TestSessionKeySupplier sks) {
            return new ResourcePrincipalAuthenticationDetailsProvider(federationClient, sks, Region.US_PHOENIX_1);
        }
    }

    /**
     * Minimal SessionKeySupplier for tests.
     */
    static final class TestSessionKeySupplier implements SessionKeySupplier {
        private volatile KeyPair keyPair;

        TestSessionKeySupplier() {
            refreshKeys();
        }

        @Override
        public KeyPair getKeyPair() {
            return keyPair;
        }

        @Override
        public void refreshKeys() {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048);
                keyPair = gen.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Basic provider that supplies a no-op custom RequestSigner so the SDK does not attempt
     * to build a default signer from key material during client construction.
     */
    static final class DummyBasicProvider implements BasicAuthenticationDetailsProvider, ProvidesCustomRequestSigner {
        private static final RequestSigner NOOP_SIGNER = (URI uri, String method, java.util.Map<String, java.util.List<String>> headers, Object body) -> java.util.Collections.emptyMap();

        @Override
        public RequestSigner getCustomRequestSigner() {
            return NOOP_SIGNER;
        }

        @Override
        public String getKeyId() {
            return "";
        }

        @Override
        public InputStream getPrivateKey() {
            return null;
        }

        @Override
        public String getPassPhrase() {
            return "";
        }

        @Override
        public char[] getPassphraseCharacters() {
            return new char[0];
        }
    }

    /**
     * Test-specific federation client that overrides makeClient to assert the builder is managed
     * by setting the SERVICE_ID property (which only works for managed providers).
     */
    static final class RecordingFederationClient extends AbstractFederationClient {
        volatile HttpClient lastClient;

        RecordingFederationClient(
            String resourcePrincipalTokenEndpoint,
            String federationEndpoint,
            SessionKeySupplier sessionKeySupplier,
            BasicAuthenticationDetailsProvider basicProvider,
            ClientConfigurator clientConfigurator,
            CircuitBreakerConfiguration circuitBreakerConfiguration
        ) {
            super(resourcePrincipalTokenEndpoint, federationEndpoint, sessionKeySupplier, basicProvider, clientConfigurator, circuitBreakerConfiguration);
        }

        @Override
        protected HttpClient makeClient(String endpoint, RequestSigner requestSigner) {
            HttpClientBuilder builder = HttpProvider.getDefault()
                .newBuilder()
                .baseUri(URI.create(endpoint))
                .registerRequestInterceptor(Priorities.AUTHENTICATION, new AuthnClientFilter(requestSigner, Collections.emptyMap()))
                .registerRequestInterceptor(Priorities.HEADER_DECORATOR, new ClientIdFilter())
                .registerRequestInterceptor(Priorities.USER, new LogHeadersFilter());

            // Critical assertion: this property can only be set if the builder is using ManagedNettyHttpProvider.
            // If unmanaged, NettyHttpClientBuilder.property(NettyClientProperties.SERVICE_ID, ...) will throw.
            builder.property(NettyClientProperties.SERVICE_ID, "test-service");

            if (clientConfigurator != null) {
                clientConfigurator.customizeClient(builder);
            }
            for (var additionalConfigurator : additionalClientConfigurator) {
                additionalConfigurator.customizeClient(builder);
            }
            lastClient = builder.build();

            return lastClient;
        }

        @Override
        protected SecurityTokenAdapter getSecurityTokenFromServer() {
            // Not exercised in this test; return a dummy token if ever called
            return new SecurityTokenAdapter("dummy", sessionKeySupplier);
        }
    }
}
