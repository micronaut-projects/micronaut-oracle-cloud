package io.micronaut.oraclecloud.httpclient.apache.core;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.micronaut.oraclecloud.httpclient.apache.core.ApacheCoreHttpClientBuilder.SOCKET_PATH_PROPERTY;

/**
 * Verifies that when ObjectStorage client is used with Apache Http Core client
 * (and Netty client excluded), the application fails to start with the expected
 * NoClassDefFoundError for HttpClientConfiguration, which we'll address later.
 */
@MicronautTest
public class ApacheObjectStorageClientTest {

    ObjectStorageClient objectStorageClient;

    static Path socketDirectory;
    static Path socketFile;
    static ServerSocketChannel server;
    static ExecutorService executor;

    @BeforeEach
    protected void bootstrap() throws Exception {
        // If prefix is too long the java.net.SocketException: Unix domain path too long will keep happening.
        socketDirectory = Files.createTempDirectory("a");
        socketFile = socketDirectory.resolve("socket");

        server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        server.bind(UnixDomainSocketAddress.of(socketFile));

        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                SocketChannel client = server.accept();
                if (client == null) {
                    return;
                }
                ByteBuffer readBuffer = ByteBuffer.allocate(8192);
                StringBuilder request = new StringBuilder();

                while (true) {
                    readBuffer.clear();
                    int bytesRead = client.read(readBuffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    readBuffer.flip();
                    byte[] bytes = new byte[bytesRead];
                    readBuffer.get(bytes);
                    request.append(new String(bytes, StandardCharsets.UTF_8));

                    if (request.toString().contains("\r\n\r\n")) {
                        break;
                    }
                }

                String body = "{\"code\":\"NotFound\",\"message\":\"fake server\"}";
                String response =
                    "HTTP/1.1 200 Not Found\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        body;

                ByteBuffer writeBuffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
                while (writeBuffer.hasRemaining()) {
                    client.write(writeBuffer);
                }

                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.setProperty(SOCKET_PATH_PROPERTY, socketFile.toAbsolutePath().toString());
    }

    @AfterEach
    void clean() throws IOException {
        server.close();
        executor.shutdownNow();
        Files.deleteIfExists(socketFile);
        Files.deleteIfExists(socketDirectory);
    }

    @Test
    void failsToStartWithApacheClientSelected() {
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "micronaut.server.port", "${random.port}"
        ))) {
            objectStorageClient = ctx.getBean(ObjectStorageClient.class);
            GetObjectResponse resp = objectStorageClient.getObject(
                GetObjectRequest.builder()
                    .bucketName("test")
                    .objectName("test")
                    .namespaceName("test")
                    .build());
            Assertions.assertNotNull(resp);
        }
    }

    @Singleton
    static class DatabaseClientBuilderListener
        implements BeanCreatedEventListener<ObjectStorageClient.Builder> {

        @Value("${micronaut.server.port}") int port;

        @Override
        public ObjectStorageClient.Builder onCreated(
            @NonNull BeanCreatedEvent<ObjectStorageClient.Builder> event
        ) {
            ObjectStorageClient.Builder builder = event.getBean();
            builder.endpoint("http://localhost:" + port);
            return builder;
        }
    }
}
