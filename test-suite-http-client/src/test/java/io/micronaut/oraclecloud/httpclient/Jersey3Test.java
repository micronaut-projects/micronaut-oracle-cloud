package io.micronaut.oraclecloud.httpclient;

import com.oracle.bmc.common.ClientBuilderBase;
import com.oracle.bmc.http.client.HttpClientBuilder;
import com.oracle.bmc.http.client.HttpProvider;
import com.oracle.bmc.http.client.StandardClientProperties;
import com.oracle.bmc.http.client.jersey3.Jersey3HttpProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Disabled;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class Jersey3Test extends NettyTest {
    private HttpProvider provider() {
        return Jersey3HttpProvider.getInstance();
    }

    private String endpoint() {
        InetSocketAddress addr = (InetSocketAddress) getServerChannel().localAddress();
        return "http://" + addr.getHostString() + ":" + addr.getPort();
    }

    @Override
    protected HttpClientBuilder newBuilder() {
        return provider().newBuilder()
            .property(StandardClientProperties.READ_TIMEOUT, Duration.ofSeconds(10))
            .baseUri(endpoint());
    }

    @Override
    protected void customize(ClientBuilderBase<?, ?> client) {
        client.httpProvider(provider())
            .endpoint(endpoint());
    }

    @Override
    protected void setupBootstrap(ServerBootstrap bootstrap) {
        bootstrap
            .channel(NioServerSocketChannel.class)
            .localAddress("127.0.0.1", 0);
    }

    @Override
    @Disabled // see super method
    public void emptyJsonBody() throws ExecutionException, InterruptedException {
        super.emptyJsonBody();
    }

    @Override
    @Disabled // breaks the next test for some reason
    public void fullSetupTest() throws CertificateException {
        super.fullSetupTest();
    }
}
