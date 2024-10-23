package io.micronaut.oraclecloud.httpclient.apache.core;

import com.oracle.bmc.common.ClientBuilderBase;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import io.micronaut.oraclecloud.httpclient.NettyTest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioServerDomainSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;

public class ApacheNettyTest extends NettyTest {
    private Path socketDirectory;
    private Path socketFile;

    HttpProvider provider() {
        return new ApacheCoreHttpProvider();
    }

    @Override
    protected final HttpClientBuilder newBuilder() {
        return provider().newBuilder()
            .baseUri("https://example.com")
            .property(ApacheCoreHttpProvider.SOCKET_PATH, socketFile);
    }

    @Override
    protected final void customize(ClientBuilderBase<?, ?> client) {
        client.httpProvider(provider())
            .additionalClientConfigurator(c -> c.property(ApacheCoreHttpProvider.SOCKET_PATH, socketFile));
    }

    @Override
    protected void setupBootstrap(ServerBootstrap bootstrap) throws Exception {
        socketDirectory = Files.createTempDirectory("oraclecloud-httpclient-apache");
        socketFile = socketDirectory.resolve("socket");
        bootstrap
            .channel(NioServerDomainSocketChannel.class)
            .localAddress(UnixDomainSocketAddress.of(socketFile));
    }

    @AfterEach
    void clean() throws IOException {
        Files.deleteIfExists(socketFile);
        Files.deleteIfExists(socketDirectory);
    }

    @Override
    @Disabled // not implemented
    public void timeoutRetryTest() throws Exception {
        super.timeoutRetryTest();
    }

    @Override
    @Disabled // not implemented
    public void connectionReuse() throws Exception {
        super.connectionReuse();
    }

    @Override
    @Disabled // not implemented
    public void fullSetupTest() throws CertificateException {
        super.fullSetupTest();
    }
}
