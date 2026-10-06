package io.micronaut.oraclecloud.core;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.http.signing.RequestSignerFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.DependencyInjectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleCloudProxySigningTest {
    @Test
    void signingIsEnabledByDefaultAndWhenExplicitlyEnabled() {
        for (Map<String, Object> signing : List.of(Map.<String, Object>of(), Map.<String, Object>of("oci.signing.enabled", true))) {
            Map<String, Object> properties = new HashMap<>(signing);
            properties.put("oci.config.enabled", false);
            properties.put("oci.tenant-id", "test-tenant");
            properties.put("oci.region", "us-phoenix-1");
            try (ApplicationContext context = ApplicationContext.run(properties)) {
                assertTrue(context.getBean(OracleCloudSigningConfigurationProperties.class).isEnabled());
                assertTrue(context.getBean(AbstractAuthenticationDetailsProvider.class) instanceof SimpleAuthenticationDetailsProvider);
                assertFalse(context.containsBean(RequestSignerFactory.class));
            }
        }
    }

    @Test
    void proxySigningDoesNotLoadCredentials(@TempDir Path directory) throws Exception {
        Path invalidConfig = Files.createFile(directory.resolve("config"));
        for (Map<String, Object> auth : List.of(
            Map.<String, Object>of(),
            Map.<String, Object>of("oci.config.path", invalidConfig.toString()),
            Map.<String, Object>of("oci.config.path", invalidConfig.toString(), "oci.config.session-token", true),
            Map.<String, Object>of("oci.tenant-id", "test-tenant"),
            Map.<String, Object>of("oci.config.instance-principal.enabled", true),
            Map.<String, Object>of("OCI_RESOURCE_PRINCIPAL_VERSION", "invalid")
        )) {
            Map<String, Object> properties = new HashMap<>(auth);
            properties.put("oci.signing.enabled", false);
            properties.put("oci.region", "phx");
            try (ApplicationContext context = ApplicationContext.run(properties)) {
                assertFalse(context.getBean(OracleCloudSigningConfigurationProperties.class).isEnabled());
                AbstractAuthenticationDetailsProvider provider = context.getBean(AbstractAuthenticationDetailsProvider.class);
                assertEquals(1, context.getBeansOfType(AbstractAuthenticationDetailsProvider.class).size());
                assertSame(provider, context.getBean(RegionProvider.class));
                assertEquals(Region.US_PHOENIX_1, ((RegionProvider) provider).getRegion());
                assertFalse(context.containsBean(ConfigFileAuthenticationDetailsProvider.class));
                assertFalse(context.containsBean(SimpleAuthenticationDetailsProvider.class));
                assertFalse(context.containsBean(SessionTokenAuthenticationDetailsProvider.class));
                assertFalse(context.containsBean(InstancePrincipalsAuthenticationDetailsProvider.class));
                assertFalse(context.containsBean(ResourcePrincipalAuthenticationDetailsProvider.class));
                var signer = context.getBean(RequestSignerFactory.class).createRequestSigner(null, provider);
                assertTrue(signer.signRequest(URI.create("https://objectstorage.us-phoenix-1.oraclecloud.com"), "GET", Map.of(), null).isEmpty());
                assertTrue(signer.signRequest(URI.create("https://objectstorage.us-phoenix-1.oraclecloud.com"), "POST", Map.of(), "body").isEmpty());
            }
        }
    }

    @Test
    void proxySigningRequiresARegion() {
        try (ApplicationContext context = ApplicationContext.run(Map.of("oci.signing.enabled", false))) {
            var failure = assertThrows(DependencyInjectionException.class, () -> context.getBean(AbstractAuthenticationDetailsProvider.class));
            assertTrue(failure.getMessage().contains("oci.region"));
        }
    }

    @Test
    void applicationProvidedBeansTakePrecedence() {
        AbstractAuthenticationDetailsProvider provider = new AbstractAuthenticationDetailsProvider() { };
        RequestSignerFactory signerFactory = (service, auth) -> (uri, method, headers, body) -> Map.of("test", "custom");
        try (ApplicationContext context = ApplicationContext.builder().properties(Map.of("oci.signing.enabled", false)).build()) {
            context.registerSingleton(AbstractAuthenticationDetailsProvider.class, provider);
            context.registerSingleton(RequestSignerFactory.class, signerFactory);
            context.start();
            assertSame(provider, context.getBean(AbstractAuthenticationDetailsProvider.class));
            assertSame(signerFactory, context.getBean(RequestSignerFactory.class));
            assertEquals(1, context.getBeansOfType(AbstractAuthenticationDetailsProvider.class).size());
            assertEquals(1, context.getBeansOfType(RequestSignerFactory.class).size());
        }
    }
}
