package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxySigningTest {
    @Test
    void managedClientsSendUnsignedRequestsWithoutCredentials() throws Exception {
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "spec.name", "ProxySigningTest",
            "use.real.auth", true,
            "oci.signing.enabled", false,
            "oci.region", "us-phoenix-1",
            "micronaut.server.port", -1
        ))) {
            EmbeddedServer server = context.getBean(EmbeddedServer.class).start();
            ObjectStorageClient client = context.getBean(ObjectStorageClient.class);
            assertTrue(client.getEndpoint().contains("us-phoenix-1"));
            client.setEndpoint(server.getURL().toString());
            assertEquals("proxy-namespace", client.getNamespace(GetNamespaceRequest.builder().build()).getValue());

            byte[] body = "proxy-body".getBytes(StandardCharsets.UTF_8);
            assertEquals(200, client.putObject(PutObjectRequest.builder()
                .namespaceName("namespace")
                .bucketName("bucket")
                .objectName("object")
                .contentLength((long) body.length)
                .putObjectBody(new ByteArrayInputStream(body))
                .build()).get__httpStatusCode__());

            ObjectStorageAsyncClient asyncClient = context.getBean(ObjectStorageAsyncClient.class);
            asyncClient.setEndpoint(server.getURL().toString());
            assertEquals("proxy-namespace", asyncClient.getNamespace(GetNamespaceRequest.builder().build(), null)
                .get(10, TimeUnit.SECONDS).getValue());
        }
    }

    @Controller("/n")
    @Requires(property = "spec.name", value = "ProxySigningTest")
    static class SigningProxy {
        @Get(produces = MediaType.APPLICATION_JSON)
        HttpResponse<byte[]> namespace(HttpRequest<?> request) {
            assertUnsigned(request);
            return HttpResponse.ok("\"proxy-namespace\"".getBytes(StandardCharsets.UTF_8));
        }

        @Put(uri = "/{namespace}/b/{bucket}/o/{object}", consumes = MediaType.ALL)
        HttpResponse<?> putObject(HttpRequest<?> request, @Body byte[] body) {
            assertUnsigned(request);
            assertArrayEquals("proxy-body".getBytes(StandardCharsets.UTF_8), body);
            return HttpResponse.ok();
        }

        private static void assertUnsigned(HttpRequest<?> request) {
            assertNull(request.getHeaders().get("Authorization"));
            assertNull(request.getHeaders().get("x-content-sha256"));
        }
    }
}
