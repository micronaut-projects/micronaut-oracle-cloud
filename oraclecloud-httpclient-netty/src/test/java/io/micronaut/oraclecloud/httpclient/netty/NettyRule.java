package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.runners.model.MultipleFailureException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NettyRule implements BeforeEachCallback, AfterEachCallback {
    private Queue<ExpectedRequestHandler> handlers;
    private String endpoint;

    boolean handleContinue;
    Consumer<Channel> channelCustomizer;
    private List<Throwable> errors;
    private Channel serverChannel;
    private NioEventLoopGroup group;

    public URI getEndpoint() {
        return URI.create(endpoint);
    }

    public void handleOneRequest(ExpectedRequestHandler handler) {
        handlers.add(handler);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ((NettyTest) context.getTestInstance().get()).netty = this;

        Thread testThread = Thread.currentThread();

        channelCustomizer = c -> {};
        handleContinue = false;
        handlers = new ArrayDeque<>();

        errors = new CopyOnWriteArrayList<>();

        group = new NioEventLoopGroup(1);

        ServerBootstrap bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(group, group)
                .localAddress("127.0.0.1", 0)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                                .addLast(new LoggingHandler(LogLevel.INFO))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(4096) {
                                    @Override
                                    protected Object newContinueResponse(HttpMessage start, int maxContentLength, ChannelPipeline pipeline) {
                                        if (!handleContinue) {
                                            return super.newContinueResponse(start, maxContentLength, pipeline);
                                        }

                                        ExpectedRequestHandler handler = handlers.poll();
                                        if (handler == null) {
                                            throw new AssertionError("Unexpected message: " + start);
                                        }
                                        ChannelHandlerContext ctx = pipeline.context(this);
                                        try {
                                            handler.handle(ctx, (HttpRequest) start);
                                        } catch (Exception e) {
                                            try {
                                                exceptionCaught(ctx, e);
                                            } catch (Exception ex) {
                                                throw new RuntimeException(ex);
                                            }
                                        }
                                        return null;
                                    }
                                })
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        if (((HttpMessage) msg).decoderResult().isFailure()) {
                                            ((HttpMessage) msg).decoderResult().cause().printStackTrace();
                                        }
                                        ExpectedRequestHandler handler = handlers.poll();
                                        if (handler == null) {
                                            throw new AssertionError("Unexpected message: " + msg);
                                        }
                                        handler.handle(ctx, (HttpRequest) msg);
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        if (cause instanceof ReadTimeoutException ||
                                                (cause instanceof IOException && cause.getMessage().equals("Connection reset by peer"))) {
                                            // not fatal
                                            ctx.close();
                                            return;
                                        }

                                        errors.add(cause);
                                        testThread.interrupt();
                                        ctx.close();
                                        ctx.channel().parent().close(); // close the server too
                                    }
                                });
                        channelCustomizer.accept(ch);
                    }
                });
        serverChannel = bootstrap.bind().syncUninterruptibly().channel();
        InetSocketAddress addr = (InetSocketAddress) serverChannel.localAddress();
        endpoint = "http://" + addr.getHostString() + ":" + addr.getPort();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        try {
            serverChannel.close();
            group.shutdownGracefully();
        } catch (Throwable t) {
            errors.add(t);
        }
        MultipleFailureException.assertEmpty(errors);
    }
}
