package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.Region;
import com.oracle.bmc.Service;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.http.signing.RequestSigner;
import com.oracle.bmc.http.signing.RequestSignerFactory;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.netty.channel.EventLoopGroupRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioServerDomainSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DomainSocketProxyTest {
    @Test
    public void test() throws Exception {
        Path tmpDir = Files.createTempDirectory("DomainSocketProxyTest");
        Path socketFile = tmpDir.resolve("sock");
        try (ApplicationContext ctx = ApplicationContext.run(Map.of(
            "spec.name", "DomainSocketProxyTest",
            "oci.region", Region.EU_FRANKFURT_1,
            "oci.netty.proxy-domain-socket", socketFile
        ))) {
            new ServerBootstrap()
                .group(ctx.getBean(EventLoopGroupRegistry.class).getDefaultEventLoopGroup(), ctx.getBean(EventLoopGroupRegistry.class).getDefaultEventLoopGroup())
                .channel(NioServerDomainSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(@NotNull Channel ch) throws Exception {
                        ch.config().setAutoRead(true);
                        ch.pipeline()
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(8192))
                            .addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
                                    System.out.println(msg);

                                    FullHttpRequest request = (FullHttpRequest) msg;

                                    Assertions.assertEquals(HttpMethod.GET, request.method());
                                    Assertions.assertEquals("https://objectstorage.eu-frankfurt-1.oraclecloud.com/n/namespaceName/b?compartmentId=compartmentId", request.uri());

                                    request.release();

                                    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer("[]", StandardCharsets.UTF_8));
                                    response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                    ctx.writeAndFlush(response);
                                }
                            });
                    }
                })
                .bind(UnixDomainSocketAddress.of(socketFile)).sync();

            ctx.getBean(ObjectStorageClient.class).listBuckets(ListBucketsRequest.builder()
                .compartmentId("compartmentId")
                .namespaceName("namespaceName")
                .build());
        } finally {
            Files.deleteIfExists(socketFile);
            Files.deleteIfExists(tmpDir);
        }
    }


    @Singleton
    @Internal
    @Requires(property = "spec.name", value = "DomainSocketProxyTest")
    @Requires(property = "oci.netty.proxy-domain-socket")
    static class MockRequestSigner implements RequestSignerFactory {
        @Override
        public RequestSigner createRequestSigner(Service service, AbstractAuthenticationDetailsProvider abstractAuthProvider) {
            return new RequestSigner() {
                @Override
                public @NotNull Map<String, String> signRequest(@NotNull URI uri, @NotNull String httpMethod, @NotNull Map<String, List<String>> headers, @Nullable Object body) {
                    return Map.of();
                }
            };
        }
    }

    @Singleton
    @Internal
    @Replaces(ConfigFileAuthenticationDetailsProvider.class)
    @Requires(property = "spec.name", value = "DomainSocketProxyTest")
    @Requires(property = "oci.netty.proxy-domain-socket")
    static class NoOpAuthDetailsProvider implements AbstractAuthenticationDetailsProvider, RegionProvider {

        private final Region region;

        NoOpAuthDetailsProvider(@NonNull @Property(name = "oci.region") String regionId) {
            this.region = Region.fromRegionId(regionId);
        }

        @Override
        public Region getRegion() {
            return region;
        }

    }
}
