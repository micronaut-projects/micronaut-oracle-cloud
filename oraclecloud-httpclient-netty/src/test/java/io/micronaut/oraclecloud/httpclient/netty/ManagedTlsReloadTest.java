package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.server.EmbeddedServer;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ManagedTlsReloadTest {

    private static void storeTrust(Path trustStorePath, SelfSignedCertificate... certs) throws IOException, GeneralSecurityException {
        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(null, null);
        for (int i = 0; i < certs.length; i++) {
            ts.setCertificateEntry("cert" + i, certs[i].cert());
        }
        try (OutputStream os = Files.newOutputStream(trustStorePath)) {
            ts.store(os, "".toCharArray());
        }
    }

    private static void storeKey(Path keyStorePath, SelfSignedCertificate cert) throws IOException, GeneralSecurityException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("key", cert.key(), "".toCharArray(), new Certificate[]{cert.cert()});
        try (OutputStream os = Files.newOutputStream(keyStorePath)) {
            ks.store(os, "".toCharArray());
        }
    }

    @Test
    public void test() throws IOException, GeneralSecurityException, ExecutionException, InterruptedException {
        Path trustStoreServer = Files.createTempFile("micronaut-test-trust-store-server", null);
        Path keyStoreServer = Files.createTempFile("micronaut-test-key-store-server", null);
        Path trustStoreClient = Files.createTempFile("micronaut-test-trust-store-client", null);
        Path keyStoreClient = Files.createTempFile("micronaut-test-key-store-client", null);

        SelfSignedCertificate client1 = new SelfSignedCertificate("client1");
        SelfSignedCertificate client2 = new SelfSignedCertificate("client2");
        storeKey(keyStoreClient, client1);
        storeTrust(trustStoreServer, client1, client2);
        SelfSignedCertificate serverCert = new SelfSignedCertificate("localhost");
        storeKey(keyStoreServer, serverCert);
        storeTrust(trustStoreClient, serverCert);

        try (ApplicationContext ctx = ApplicationContext.run(Map.ofEntries(
            Map.entry("spec.name", "ManagedTlsReloadTest"),
            Map.entry("micronaut.ssl.enabled", "true"),
            Map.entry("micronaut.server.ssl.port", "-1"),
            Map.entry("micronaut.server.ssl.client-authentication", "NEED"),
            Map.entry("micronaut.http.services.oci.ssl.enabled", "true"),
            Map.entry("micronaut.http.services.oci.ssl.client-authentication", "NEED"),

            Map.entry("micronaut.server.ssl.trust-store.path", "file://" + trustStoreServer),
            Map.entry("micronaut.server.ssl.trust-store.type", "JKS"),
            Map.entry("micronaut.server.ssl.trust-store.password", ""),

            Map.entry("micronaut.server.ssl.key-store.path", "file://" + keyStoreServer),
            Map.entry("micronaut.server.ssl.key-store.type", "PKCS12"),
            Map.entry("micronaut.server.ssl.key-store.password", ""),

            Map.entry("micronaut.http.services.oci.ssl.trust-store.path", "file://" + trustStoreClient),
            Map.entry("micronaut.http.services.oci.ssl.trust-store.type", "JKS"),
            Map.entry("micronaut.http.services.oci.ssl.trust-store.password", ""),

            Map.entry("micronaut.http.services.oci.ssl.key-store.path", "file://" + keyStoreClient),
            Map.entry("micronaut.http.services.oci.ssl.key-store.type", "PKCS12"),
            Map.entry("micronaut.http.services.oci.ssl.key-store.password", "")
        ));
             EmbeddedServer embeddedServer = ctx.getBean(EmbeddedServer.class)) {
            embeddedServer.start();

            try (HttpClient client = ctx.getBean(HttpProvider.class).newBuilder()
                .baseUri(embeddedServer.getURI())
                .build()) {
                try (HttpResponse response = client.createRequest(Method.GET).appendPathPart("/cert").execute().toCompletableFuture().get()) {
                    Assertions.assertEquals("CN=client1", response.textBody().toCompletableFuture().get());
                }
                storeKey(keyStoreClient, client2);
                ctx
                    .getBean(Argument.of(ApplicationEventPublisher.class, RefreshEvent.class))
                    .publishEvent(new RefreshEvent(Map.of("micronaut.http.services.oci.ssl", "")));
                try (HttpResponse response = client.createRequest(Method.GET).appendPathPart("/cert").execute().toCompletableFuture().get()) {
                    Assertions.assertEquals("CN=client2", response.textBody().toCompletableFuture().get());
                }
            }
        } finally {
            Files.deleteIfExists(trustStoreServer);
            Files.deleteIfExists(keyStoreServer);
            Files.deleteIfExists(trustStoreClient);
            Files.deleteIfExists(keyStoreClient);
        }
    }

    @Test
    public void customServiceId() throws IOException, GeneralSecurityException, ExecutionException, InterruptedException {
        Path trustStoreServer = Files.createTempFile("micronaut-test-trust-store-server", null);
        Path keyStoreServer = Files.createTempFile("micronaut-test-key-store-server", null);
        Path keyStoreClientCustom = Files.createTempFile("micronaut-test-key-store-client-custom", null);
        Path keyStoreClientDefault = Files.createTempFile("micronaut-test-key-store-client-default", null);

        SelfSignedCertificate clientCustom = new SelfSignedCertificate("clientCustom");
        storeKey(keyStoreClientCustom, clientCustom);
        SelfSignedCertificate clientDefault = new SelfSignedCertificate("clientDefault");
        storeKey(keyStoreClientDefault, clientDefault);
        storeTrust(trustStoreServer, clientCustom, clientDefault);
        SelfSignedCertificate serverCert = new SelfSignedCertificate("localhost");
        storeKey(keyStoreServer, serverCert);

        try (ApplicationContext ctx = ApplicationContext.run(Map.ofEntries(
            Map.entry("spec.name", "ManagedTlsReloadTest"),
            Map.entry("micronaut.ssl.enabled", "true"),
            Map.entry("micronaut.server.ssl.port", "-1"),
            Map.entry("micronaut.server.ssl.client-authentication", "NEED"),

            Map.entry("micronaut.server.ssl.trust-store.path", "file://" + trustStoreServer),
            Map.entry("micronaut.server.ssl.trust-store.type", "JKS"),
            Map.entry("micronaut.server.ssl.trust-store.password", ""),

            Map.entry("micronaut.server.ssl.key-store.path", "file://" + keyStoreServer),
            Map.entry("micronaut.server.ssl.key-store.type", "PKCS12"),
            Map.entry("micronaut.server.ssl.key-store.password", ""),

            Map.entry("micronaut.http.services.custom.ssl.enabled", "true"),
            Map.entry("micronaut.http.services.custom.ssl.client-authentication", "NEED"),
            Map.entry("micronaut.http.services.custom.ssl.insecure-trust-all-certificates", true),
            Map.entry("micronaut.http.services.custom.ssl.key-store.path", "file://" + keyStoreClientCustom),
            Map.entry("micronaut.http.services.custom.ssl.key-store.type", "PKCS12"),
            Map.entry("micronaut.http.services.custom.ssl.key-store.password", ""),

            Map.entry("micronaut.http.services.oci.ssl.enabled", "true"),
            Map.entry("micronaut.http.services.oci.ssl.client-authentication", "NEED"),
            Map.entry("micronaut.http.services.oci.ssl.insecure-trust-all-certificates", true),
            Map.entry("micronaut.http.services.oci.ssl.key-store.path", "file://" + keyStoreClientDefault),
            Map.entry("micronaut.http.services.oci.ssl.key-store.type", "PKCS12"),
            Map.entry("micronaut.http.services.oci.ssl.key-store.password", "")
        ));
             EmbeddedServer embeddedServer = ctx.getBean(EmbeddedServer.class)) {
            embeddedServer.start();

            try (HttpClient client = ctx.getBean(HttpProvider.class).newBuilder()
                .baseUri(embeddedServer.getURI())
                .build()) {
                try (HttpResponse response = client.createRequest(Method.GET).appendPathPart("/cert").execute().toCompletableFuture().get()) {
                    Assertions.assertEquals("CN=clientDefault", response.textBody().toCompletableFuture().get());
                }
            }

            try (HttpClient client = ctx.getBean(HttpProvider.class).newBuilder()
                .property(NettyClientProperties.SERVICE_ID, "custom")
                .baseUri(embeddedServer.getURI())
                .build()) {
                try (HttpResponse response = client.createRequest(Method.GET).appendPathPart("/cert").execute().toCompletableFuture().get()) {
                    Assertions.assertEquals("CN=clientCustom", response.textBody().toCompletableFuture().get());
                }
            }
        } finally {
            Files.deleteIfExists(trustStoreServer);
            Files.deleteIfExists(keyStoreServer);
            Files.deleteIfExists(keyStoreClientCustom);
            Files.deleteIfExists(keyStoreClientDefault);
        }
    }

    @Controller
    @Requires(property = "spec.name", value = "ManagedTlsReloadTest")
    public static class MyController {
        @Get("/cert")
        public String cert(HttpRequest<?> request) {
            X509Certificate certificate = (X509Certificate) request.getCertificate().get();
            return certificate.getSubjectDN().toString();
        }
    }
}
