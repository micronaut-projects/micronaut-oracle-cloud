package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpClient;
import com.oracle.bmc.http.client.KeyStoreWithPassword;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.StandardClientProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.InetNameResolver;
import io.netty.resolver.InetSocketAddressResolver;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("NullableProblems")
public class TlsTest {
    @Test
    public void sslContext() throws Exception {
        // set an ssl context with a self-signed cert

        SelfSignedCertificate cert = new SelfSignedCertificate();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(cert.key(), cert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("foo", cert.cert());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://localhost:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.SSL_CONTEXT, sslContext)
                .build()) {
            client.createRequest(Method.GET).execute().toCompletableFuture().get();
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void trustStore() throws Exception {
        // set a trust store with a self-signed cert

        SelfSignedCertificate cert = new SelfSignedCertificate();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(cert.key(), cert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("foo", cert.cert());

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://localhost:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.TRUST_STORE, trustStore)
                .build()) {
            client.createRequest(Method.GET).execute().toCompletableFuture().get();
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void wrongTrustStore() throws Exception {
        // set a trust store with a *different* self-signed cert, this fails

        SelfSignedCertificate cert = new SelfSignedCertificate();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(cert.key(), cert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("foo", new SelfSignedCertificate().cert()); // different cert

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://localhost:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.TRUST_STORE, trustStore)
                .build()) {
            ExecutionException e = Assertions.assertThrows(
                    ExecutionException.class,
                    () -> client.createRequest(Method.GET).execute().toCompletableFuture().get());
            Assertions.assertTrue(e.getCause().getCause() instanceof SSLHandshakeException);
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void keyStore() throws Exception {
        // set a trust store, and a key store for the client cert

        SelfSignedCertificate serverCert = new SelfSignedCertificate();
        SelfSignedCertificate clientCert = new SelfSignedCertificate();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(serverCert.key(), serverCert.cert())
                                        .clientAuth(ClientAuth.REQUIRE)
                                        .trustManager(clientCert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setKeyEntry("key", clientCert.key(), "pw".toCharArray(), new Certificate[]{clientCert.cert()});

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("foo", serverCert.cert());

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://localhost:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.TRUST_STORE, trustStore)
                .property(StandardClientProperties.KEY_STORE, new KeyStoreWithPassword(clientKeyStore, "pw"))
                .build()) {
            client.createRequest(Method.GET).execute().toCompletableFuture().get();
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void hostnameVerifier() throws Exception {
        // set a trust store, and a hostname verifier to accept the cert for a different domain

        SelfSignedCertificate serverCert = new SelfSignedCertificate("1.example.com");

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(serverCert.key(), serverCert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("foo", serverCert.cert());

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://2.example.com:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.TRUST_STORE, trustStore)
                .property(StandardClientProperties.HOSTNAME_VERIFIER, (hostname, session) -> {
                    try {
                        // allow the cert for 1.example.com for 2.example.com.
                        for (Certificate peerCertificate : session.getPeerCertificates()) {
                            if (((X509Certificate) peerCertificate).getSubjectX500Principal().getName().equals("CN=1.example.com") &&
                                    hostname.equals("2.example.com")) {
                                return true;
                            }
                        }
                        return false;
                    } catch (SSLPeerUnverifiedException e) {
                        return false;
                    }
                })
                .build()) {
            ((NettyHttpClient) client).bootstrap.resolver(new MockAddressResolverGroup("2.example.com", InetAddress.getByName("127.0.0.1")));

            client.createRequest(Method.GET).execute().toCompletableFuture().get();
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void hostnameVerifierSelfSigned() throws Exception {
        // do *NOT* set a trust store, and a hostname verifier to accept the cert for a different domain.
        // this verifies that we don't mind if no known CA signed the cert. the hostname verifier should do *all*
        // validation.

        SelfSignedCertificate serverCert = new SelfSignedCertificate("1.example.com");

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(serverCert.key(), serverCert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://2.example.com:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.HOSTNAME_VERIFIER, (hostname, session) -> {
                    try {
                        // allow the cert for 1.example.com for 2.example.com.
                        for (Certificate peerCertificate : session.getPeerCertificates()) {
                            if (((X509Certificate) peerCertificate).getSubjectX500Principal().getName().equals("CN=1.example.com") &&
                                    hostname.equals("2.example.com")) {
                                return true;
                            }
                        }
                        return false;
                    } catch (SSLPeerUnverifiedException e) {
                        return false;
                    }
                })
                .build()) {
            ((NettyHttpClient) client).bootstrap.resolver(new MockAddressResolverGroup("2.example.com", InetAddress.getByName("127.0.0.1")));

            client.createRequest(Method.GET).execute().toCompletableFuture().get();
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void hostnameVerifierFail() throws Exception {
        // set a trust store, and a hostname verifier that fails.

        SelfSignedCertificate serverCert = new SelfSignedCertificate("1.example.com");

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(serverCert.key(), serverCert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("foo", serverCert.cert());

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://2.example.com:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.TRUST_STORE, trustStore)
                .property(StandardClientProperties.HOSTNAME_VERIFIER, (hostname, session) -> false)
                .build()) {
            ((NettyHttpClient) client).bootstrap.resolver(new MockAddressResolverGroup("2.example.com", InetAddress.getByName("127.0.0.1")));

            ExecutionException e = Assertions.assertThrows(
                    ExecutionException.class,
                    () -> client.createRequest(Method.GET).execute().toCompletableFuture().get());
            Assertions.assertTrue(e.getCause().getCause() instanceof SSLHandshakeException);
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void wrongHostName() throws Exception {
        // set a trust store, but with the wrong domain, so the handshake fails.

        SelfSignedCertificate serverCert = new SelfSignedCertificate("1.example.com");

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Channel serverChannel = new ServerBootstrap()
                .group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(SslContextBuilder.forServer(serverCert.key(), serverCert.cert())
                                        .build()
                                        .newHandler(ch.alloc()))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(1024))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        ((FullHttpRequest) msg).release();
                                        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                                    }
                                });
                    }
                })
                .bind(0).syncUninterruptibly().channel();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("foo", serverCert.cert());

        try (HttpClient client = new NettyHttpProvider().newBuilder()
                .baseUri(URI.create("https://2.example.com:" + ((ServerSocketChannel) serverChannel).localAddress().getPort()))
                .property(StandardClientProperties.TRUST_STORE, trustStore)
                .build()) {
            ((NettyHttpClient) client).bootstrap.resolver(new MockAddressResolverGroup("2.example.com", InetAddress.getByName("127.0.0.1")));

            ExecutionException e = Assertions.assertThrows(
                    ExecutionException.class,
                    () -> client.createRequest(Method.GET).execute().toCompletableFuture().get());
            Assertions.assertTrue(e.getCause().getCause() instanceof SSLHandshakeException);
        } finally {
            serverChannel.close();
            group.shutdownGracefully();
        }
    }

    @Test
    public void badSsl() {
        for (String uri : Arrays.asList(
                "https://expired.badssl.com/",
                "https://wrong.host.badssl.com/",
                "https://self-signed.badssl.com/",
                "https://untrusted-root.badssl.com/",
                //"https://revoked.badssl.com/", not implemented
                //"https://pinning-test.badssl.com/", not implemented
                "https://no-subject.badssl.com/",
                "https://reversed-chain.badssl.com/",
                "https://rc4-md5.badssl.com/",
                "https://rc4.badssl.com/",
                "https://3des.badssl.com/",
                "https://null.badssl.com/",
                "https://dh480.badssl.com/",
                "https://dh512.badssl.com/",
                "https://dh1024.badssl.com/",
                "https://dh-small-subgroup.badssl.com/",
                "https://dh-composite.badssl.com/"
        )) {
            try (HttpClient client = new NettyHttpProvider().newBuilder()
                    .baseUri(URI.create(uri))
                    .build()) {
                ExecutionException e = Assertions.assertThrows(
                        ExecutionException.class,
                        () -> client.createRequest(Method.GET).execute().toCompletableFuture().get(),
                        uri);
                Assertions.assertTrue(e.getCause().getCause() instanceof SSLHandshakeException);
            }
        }
    }

    /**
     * AddressResolverGroup that returns a fixed address for a fixed host. The tests use this to make *.example.com
     * resolve to 127.0.0.1
     */
    private static class MockAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {
        private final String expectedHost;
        private final InetAddress address;

        MockAddressResolverGroup(String expectedHost, InetAddress address) {
            this.expectedHost = expectedHost;
            this.address = address;
        }

        @Override
        protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
            return new InetSocketAddressResolver(executor, new InetNameResolver(executor) {
                @Override
                protected void doResolve(String inetHost, Promise<InetAddress> promise) {
                    Assertions.assertEquals(expectedHost, inetHost);
                    promise.setSuccess(address);
                }

                @Override
                protected void doResolveAll(String inetHost, Promise<List<InetAddress>> promise) {
                    Assertions.assertEquals(expectedHost, inetHost);
                    promise.setSuccess(Collections.singletonList(address));
                }
            });
        }
    }
}
