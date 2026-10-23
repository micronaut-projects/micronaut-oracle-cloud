package io.micronaut.oraclecloud.oke.workload.identity;

import com.oracle.bmc.auth.DefaultServiceAccountTokenProvider;
import com.oracle.bmc.auth.SessionKeySupplier;
import com.oracle.bmc.auth.SuppliedServiceAccountTokenProvider;
import com.oracle.bmc.auth.okeworkloadidentity.OkeWorkloadIdentityAuthenticationDetailsProvider;
import com.oracle.bmc.auth.okeworkloadidentity.internal.OkeTenancyOnlyAuthenticationDetailsProvider;
import com.oracle.bmc.circuitbreaker.CircuitBreakerConfiguration;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpProviderCapability;
import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.Method;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MicronautOkeWorkloadIdentityResourcePrincipalsFederationClientTest {

    public static String CERTIFICATE_STRING = """
-----BEGIN CERTIFICATE-----
MIIFajCCBFKgAwIBAgIUdqm3iigytoj0A4qjzeqqcyUUiQQwDQYJKoZIhvcNAQEL
BQAwEDEOMAwGA1UEAwwFdGVzdDEwHhcNMjMwNTMwMTcyMDQyWhcNMjMwODE1MDAw
MDAwWjAUMRIwEAYDVQQDDAlNaWNyb25hdXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDaz3fk1bp7xmAY7wRYdu6998ioDEkwemwvr3/8OR5Ij+56tGcO
gSAu7HALtcGHYB91NCO3UlDgnauBZ5Crm7HMFdBLUCxfBoxn4s+oNO5EocPbPqru
n++V5FMcXCmj5FagO+TshSzyBl/QMC1SqnIiQ7VUlO/Zhsr2q4DKiCz1v6eT5WbF
ly8eH36M3//q7sb2qFEvOUaj4NhjG+LdbQll04EGUPhIPeBkuWS714EDjKtUv35+
Ncvd285q6MfPihku3mLQRLD6nHT97mPi0VMnOp1A3VTDnTx7kjXGJsY9oSE2X0PO
xACWnyV5a61HqAa8TpE4h2MBksI9nuRFz7dxAgMBAAGjggK2MIICsjAOBgNVHQ8B
Af8EBAMCBaAwDAYDVR0TAQH/BAIwADAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAgYI
KwYBBQUHAwEwggEzBgNVHQ4EggEqBIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBANrPd+TVunvGYBjvBFh27r33yKgMSTB6bC+vf/w5HkiP7nq0Zw6B
IC7scAu1wYdgH3U0I7dSUOCdq4FnkKubscwV0EtQLF8GjGfiz6g07kShw9s+qu6f
75XkUxxcKaPkVqA75OyFLPIGX9AwLVKqciJDtVSU79mGyvargMqILPW/p5PlZsWX
Lx4ffozf/+ruxvaoUS85RqPg2GMb4t1tCWXTgQZQ+Eg94GS5ZLvXgQOMq1S/fn41
y93bzmrox8+KGS7eYtBEsPqcdP3uY+LRUyc6nUDdVMOdPHuSNcYmxj2hITZfQ87E
AJafJXlrrUeoBrxOkTiHYwGSwj2e5EXPt3ECAwEAATCCATcGA1UdIwSCAS4wggEq
gIIBJjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANHglJHO6O56lMIp
DUCff+vMWhukXlnZTq4kS7wpiJdMSTYFzN8kOKLDRujLneAfyrYDW9Tm4xFYBZRX
uRiCq7H7ck+HFR0Xq5fuKzp5j/KaGxMbloSGOWqdcWvHOSMwAr4mmGui4le14nXI
fUYZcZuXwooEHSHOMkVrZUa/6M0lNNoTWvUCj74HAXJNAEu7D+hfcKOQ0E7WbSYR
Yol+Kjw3ij6HTGPIjUpY8emDd4tWkJx3pzuTUIABCAcqz9al/Ok3ac2ZEPQbM83N
4JbWrioj+ic8nkA+xvd8NYmdbfU9fiuUXOeQQMmUeAqlgmE3nC+zfSihLYaLJ61A
skTL7a0CAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIswvhJX2y0YZiiFJy5b6KPID
SJuD+UZmIiDs61ZL6IK3iNRMnJX0IgTQMi5Ema1svvFCMM6502fMm5ktDSEr/GXY
KvqeJ4VmcgfCAxSE5tDek5/KpKOw1X57jqdoMJ25xLWTneN4YeTJDeTAG56T5/Ci
nzsQ7KiJuJc/zsKCgscZ/bQocbz3Pn9jPZTBktCdqsCvVOqHB0RuaclQxWfKsRN4
52Xov7EGYbm0xfifdg2HLnZFUzOnVAEQNsnKXW095IGFgUYlonuQ+jotRfLEf3hm
IgQuEdz+6WvdabYC1igIWN9od6fnoNI3NSRwuttvnJVWX4FkVnhu1YRdGdNkGg==
-----END CERTIFICATE-----""";

    @Test
    void testThatClientIsInitialised() throws IOException {
        File file = File.createTempFile("temp", ".ca");
        MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder.setOkeHttpClientConfiguration(new OkeHttpClientConfiguration());
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(CERTIFICATE_STRING);
        writer.flush();
        OkeNettyClientSslBuilder okeNettyClientSslBuilder = MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(file.getAbsolutePath());

        try (MockedStatic<MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient> utilities = Mockito.mockStatic(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.class, CALLS_REAL_METHODS)) {
            utilities.when(() -> MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.KUBERNETES_SERVICE_ACCOUNT_CERT_PATH)).thenReturn(okeNettyClientSslBuilder);

            try (MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient micronautOkeWorkloadIdentityResourcePrincipalsFederationClient = new MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient(
                mock(SessionKeySupplier.class),
                new DefaultServiceAccountTokenProvider(file.getAbsolutePath()),
                mock(OkeTenancyOnlyAuthenticationDetailsProvider.class),
                mock(ClientConfigurator.class),
                mock(CircuitBreakerConfiguration.class),
                new ArrayList<>(),
                new ArrayList<>()
            )) {
                try (var defaultHttpClient = micronautOkeWorkloadIdentityResourcePrincipalsFederationClient.defaultHttpClient()) {
                    Assertions.assertNotNull(defaultHttpClient);
                }
            }
        }

        file.deleteOnExit();
    }

    @Test
    void closesOwnedHttpClientResourcesExactlyOnce() throws IOException {
        HttpClient delegate = mock(HttpClient.class);
        Closeable rawHttpClient = mock(Closeable.class);
        ExecutorService ioExecutor = mock(ExecutorService.class);

        HttpClient client = MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.ownedHttpClient(
            delegate,
            rawHttpClient,
            ioExecutor
        );

        client.close();
        client.close();

        verify(delegate).close();
        verify(rawHttpClient).close();
        verify(ioExecutor).shutdown();
    }

    @Test
    void delegatesHttpClientOperations() {
        HttpClient delegate = mock(HttpClient.class);
        Closeable rawHttpClient = mock(Closeable.class);
        ExecutorService ioExecutor = mock(ExecutorService.class);
        HttpRequest request = mock(HttpRequest.class);
        HttpRequest requestWithOptions = mock(HttpRequest.class);
        Method method = Method.GET;
        Map<String, Object> requiredParameters = Map.of("request", "value");
        Map<String, Boolean> options = Map.of("option", true);
        Exception exception = new IOException("request failed");
        HttpProviderCapability capability = mock(HttpProviderCapability.class);

        when(delegate.createRequest(method)).thenReturn(request);
        when(delegate.createRequest(method, requiredParameters, options)).thenReturn(requestWithOptions);
        when(delegate.isProcessingException(exception)).thenReturn(true);
        when(delegate.hasCapability(capability)).thenReturn(true);

        try (HttpClient client = MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.ownedHttpClient(
            delegate,
            rawHttpClient,
            ioExecutor
        )) {
            Assertions.assertSame(request, client.createRequest(method));
            Assertions.assertSame(requestWithOptions, client.createRequest(method, requiredParameters, options));
            Assertions.assertTrue(client.isProcessingException(exception));
            Assertions.assertTrue(client.hasCapability(capability));
            client.updateEndpoint("https://example.com");
        }

        verify(delegate).createRequest(method);
        verify(delegate).createRequest(method, requiredParameters, options);
        verify(delegate).isProcessingException(exception);
        verify(delegate).hasCapability(capability);
        verify(delegate).updateEndpoint("https://example.com");
    }

    @Test
    void preservesRuntimeFailureAndClosesEveryOwnedResource() throws Exception {
        HttpClient delegate = mock(HttpClient.class);
        AutoCloseable rawHttpClient = mock(AutoCloseable.class);
        ExecutorService ioExecutor = mock(ExecutorService.class);
        RuntimeException delegateFailure = new RuntimeException("delegate close failed");
        Error executorFailure = new AssertionError("executor close failed");
        Exception rawFailure = new Exception("raw client close failed");
        doThrow(delegateFailure).when(delegate).close();
        doThrow(executorFailure).when(ioExecutor).shutdown();
        doThrow(rawFailure).when(rawHttpClient).close();

        HttpClient client = MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.ownedHttpClient(
            delegate,
            rawHttpClient,
            ioExecutor
        );

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, client::close);

        Assertions.assertSame(delegateFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[]{executorFailure, rawFailure}, thrown.getSuppressed());
        verify(delegate).close();
        verify(ioExecutor).shutdown();
        verify(rawHttpClient).close();
    }

    @Test
    void preservesErrorFailureAndWrapsCheckedFailure() throws Exception {
        HttpClient delegate = mock(HttpClient.class);
        AutoCloseable rawHttpClient = mock(AutoCloseable.class);
        ExecutorService ioExecutor = mock(ExecutorService.class);
        Error delegateFailure = new AssertionError("delegate close failed");
        Exception rawFailure = new Exception("raw client close failed");
        doThrow(delegateFailure).when(delegate).close();
        doThrow(rawFailure).when(rawHttpClient).close();

        HttpClient client = MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.ownedHttpClient(
            delegate,
            rawHttpClient,
            ioExecutor
        );

        Error thrown = Assertions.assertThrows(Error.class, client::close);

        Assertions.assertSame(delegateFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[]{rawFailure}, thrown.getSuppressed());
        verify(delegate).close();
        verify(ioExecutor).shutdown();
        verify(rawHttpClient).close();
    }

    @Test
    void wrapsCheckedFailureAfterClosingAllOwnedResources() throws Exception {
        HttpClient delegate = mock(HttpClient.class);
        AutoCloseable rawHttpClient = mock(AutoCloseable.class);
        ExecutorService ioExecutor = mock(ExecutorService.class);
        Exception rawFailure = new Exception("raw client close failed");
        doThrow(rawFailure).when(rawHttpClient).close();

        HttpClient client = MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.ownedHttpClient(
            delegate,
            rawHttpClient,
            ioExecutor
        );

        IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, client::close);

        Assertions.assertSame(rawFailure, thrown.getCause());
        verify(delegate).close();
        verify(ioExecutor).shutdown();
        verify(rawHttpClient).close();
    }

    @Test
    void closesResourcesWhenClientCreationFails() throws IOException {
        Closeable rawHttpClient = mock(Closeable.class);
        ExecutorService ioExecutor = mock(ExecutorService.class);
        RuntimeException failure = new RuntimeException("client creation failed");

        MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.closeOwnedResources(
            rawHttpClient,
            ioExecutor,
            failure
        );

        verify(rawHttpClient).close();
        verify(ioExecutor).shutdownNow();
    }

    @Test
    void closesResourcesWhenClientCreationHasNoAllocatedResources() {
        RuntimeException failure = new RuntimeException("client creation failed");

        Assertions.assertDoesNotThrow(() ->
            MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.closeOwnedResources(null, null, failure)
        );
    }

    @Test
    void doesNotSelfSuppressStartupFailureWhenResourceCloseRepeatsIt() throws Exception {
        AutoCloseable rawHttpClient = mock(AutoCloseable.class);
        RuntimeException failure = new RuntimeException("client creation failed");
        doThrow(failure).when(rawHttpClient).close();

        MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.closeOwnedResources(
            rawHttpClient,
            null,
            failure
        );

        Assertions.assertEquals(0, failure.getSuppressed().length);
        verify(rawHttpClient).close();
    }

    @Test
    void closesResourcesWhenActualClientConstructionFails() throws IOException {
        File file = File.createTempFile("temp", ".ca");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(CERTIFICATE_STRING);
        }
        OkeNettyClientSslBuilder sslBuilder =
            MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(file.getAbsolutePath());
        RuntimeException failure = new IllegalStateException("client customization failed");
        ClientConfigurator clientConfigurator = builder -> {
            throw failure;
        };

        try (MockedStatic<MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient> utilities =
                 Mockito.mockStatic(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.class, CALLS_REAL_METHODS)) {
            utilities.when(() -> MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(
                MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.KUBERNETES_SERVICE_ACCOUNT_CERT_PATH
            )).thenReturn(sslBuilder);

            Assertions.assertSame(
                failure,
                Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient(
                        mock(SessionKeySupplier.class),
                        new DefaultServiceAccountTokenProvider(file.getAbsolutePath()),
                        mock(OkeTenancyOnlyAuthenticationDetailsProvider.class),
                        clientConfigurator,
                        mock(CircuitBreakerConfiguration.class),
                        new ArrayList<>(),
                        new ArrayList<>()
                    )
                )
            );
        } finally {
            file.delete();
        }
    }

    @Test
    void factoryClosesFederationClient() throws IOException {
        File file = File.createTempFile("temp", ".ca");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(CERTIFICATE_STRING);
        }
        OkeNettyClientSslBuilder sslBuilder =
            MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(file.getAbsolutePath());

        MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder builder =
            new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder();
        builder.federationEndpoint("https://localhost:12250");
        builder.isTokenCachingEnabled(false);
        OkeWorkloadIdentityConfiguration configuration = new OkeWorkloadIdentityConfiguration(new OkeHttpClientConfiguration()) {
            @Override
            public OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder getBuilder() {
                return builder;
            }
        };

        try (MockedStatic<MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient> utilities = Mockito.mockStatic(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.class, CALLS_REAL_METHODS)) {
            utilities.when(() -> MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.KUBERNETES_SERVICE_ACCOUNT_CERT_PATH)).thenReturn(sslBuilder);

            OkeWorkloadIdentityFactory factory = new OkeWorkloadIdentityFactory();
            Assertions.assertNotNull(factory.okeWorkloadIdentityAuthenticationDetailsProvider(configuration));
            MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient client = builder.getMicronautFederationClient();
            Assertions.assertFalse(client.isClosed());

            factory.close();
            factory.close();

            Assertions.assertTrue(client.isClosed());
        } finally {
            file.delete();
        }
    }

    @Test
    void factoryClosesFederationClientWhenProviderStartupFails() throws IOException {
        File file = File.createTempFile("temp", ".ca");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(CERTIFICATE_STRING);
        }
        OkeNettyClientSslBuilder sslBuilder =
            MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(file.getAbsolutePath());

        FailingBuilder builder = new FailingBuilder();
        builder.federationEndpoint("https://localhost:12250");
        builder.isTokenCachingEnabled(false);
        OkeWorkloadIdentityConfiguration configuration = new OkeWorkloadIdentityConfiguration(new OkeHttpClientConfiguration()) {
            @Override
            public OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder getBuilder() {
                return builder;
            }
        };

        try (MockedStatic<MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient> utilities = Mockito.mockStatic(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.class, CALLS_REAL_METHODS)) {
            utilities.when(() -> MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.okeNettyClientSslBuilder(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.KUBERNETES_SERVICE_ACCOUNT_CERT_PATH)).thenReturn(sslBuilder);

            OkeWorkloadIdentityFactory factory = new OkeWorkloadIdentityFactory();
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> factory.okeWorkloadIdentityAuthenticationDetailsProvider(configuration)
            );

            Assertions.assertTrue(builder.getMicronautFederationClient().isClosed());
        } finally {
            file.delete();
        }
    }

    @Test
    void factoryAcceptsAnExternallyProvidedSdkBuilder() {
        var builder = mock(OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder.class);
        var configuration = mock(OkeWorkloadIdentityConfiguration.class);
        var provider = mock(OkeWorkloadIdentityAuthenticationDetailsProvider.class);
        when(configuration.getBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(provider);

        var factory = new OkeWorkloadIdentityFactory();
        Assertions.assertSame(provider, factory.okeWorkloadIdentityAuthenticationDetailsProvider(configuration));
        factory.close();
    }

    @Test
    void externalSdkBuilderFailureIsPropagated() {
        var builder = mock(OkeWorkloadIdentityAuthenticationDetailsProvider.OkeWorkloadIdentityAuthenticationDetailsProviderBuilder.class);
        var configuration = mock(OkeWorkloadIdentityConfiguration.class);
        var failure = new IllegalStateException("provider startup failed");
        when(configuration.getBuilder()).thenReturn(builder);
        when(builder.build()).thenThrow(failure);

        var factory = new OkeWorkloadIdentityFactory();

        Assertions.assertSame(
            failure,
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> factory.okeWorkloadIdentityAuthenticationDetailsProvider(configuration)
            )
        );
        factory.close();
    }

    @Test
    void factoryPreservesProviderFailureWhenFederationCleanupFails() {
        var builder = spy(new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder());
        var federationClient = mock(MicronautOkeWorkloadIdentityResourcePrincipalsFederationClient.class);
        var configuration = mock(OkeWorkloadIdentityConfiguration.class);
        var providerFailure = new IllegalStateException("provider startup failed");
        var cleanupFailure = new IllegalStateException("federation cleanup failed");
        doReturn(federationClient).when(builder).getMicronautFederationClient();
        doThrow(providerFailure).when(builder).build();
        doThrow(cleanupFailure).when(federationClient).close();
        when(configuration.getBuilder()).thenReturn(builder);

        var factory = new OkeWorkloadIdentityFactory();

        IllegalStateException thrown = Assertions.assertThrows(
            IllegalStateException.class,
            () -> factory.okeWorkloadIdentityAuthenticationDetailsProvider(configuration)
        );

        Assertions.assertSame(providerFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[]{cleanupFailure}, thrown.getSuppressed());
        verify(federationClient).close();
    }

    @Test
    void builderWithoutFederationClientCanBeClosed() {
        var builder = new MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder();

        Assertions.assertNull(builder.getMicronautFederationClient());
        Assertions.assertDoesNotThrow(builder::closeFederationClient);
    }

    private static final class FailingBuilder extends MicronautOkeWorkloadIdentityAuthenticationDetailsProviderBuilder {
        @Override
        public OkeWorkloadIdentityAuthenticationDetailsProvider build() {
            super.build();
            throw new IllegalStateException("provider startup failed");
        }
    }

}
