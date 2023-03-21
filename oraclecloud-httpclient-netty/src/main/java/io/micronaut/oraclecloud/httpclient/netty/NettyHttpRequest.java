/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.http.client.HttpRequest;
import com.oracle.bmc.http.client.HttpResponse;
import com.oracle.bmc.http.client.Method;
import com.oracle.bmc.http.client.RequestInterceptor;
import io.micronaut.oraclecloud.serde.MicronautSerdeObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

final class NettyHttpRequest implements HttpRequest {
    private static final long UNKNOWN_CONTENT_LENGTH = -1;

    private final NettyHttpClient client;

    private final Map<String, Object> attributes;

    private final Method method;
    private final HttpHeaders headers;

    private final StringBuilder uri;
    private final StringBuilder query;

    private Executor offloadExecutor;

    private Object returningBody;
    private ByteBuf immediateBody;
    private InputStream blockingBody;
    private long blockingContentLength;

    public NettyHttpRequest(NettyHttpClient nettyHttpClient, Method method) {
        client = nettyHttpClient;
        this.method = method;
        this.uri = new StringBuilder(client.baseUri.toString());
        attributes = new HashMap<>();
        headers = new DefaultHttpHeaders();
        query = new StringBuilder();
    }

    private NettyHttpRequest(NettyHttpRequest from) {
        this.client = from.client;
        this.attributes = new HashMap<>(from.attributes);
        this.method = from.method;
        this.headers = from.headers.copy();
        this.uri = new StringBuilder(from.uri);
        this.query = new StringBuilder(from.query);
        this.offloadExecutor = from.offloadExecutor;

        this.returningBody = from.returningBody;
        this.immediateBody = from.immediateBody == null ? null : from.immediateBody.retainedDuplicate();
        this.blockingBody = from.blockingBody;
        this.blockingContentLength = from.blockingContentLength;
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public HttpRequest body(Object body) {
        immediateBody = null;
        blockingBody = null;

        if (body instanceof String) {
            immediateBody = ByteBufUtil.encodeString(client.alloc(), CharBuffer.wrap((CharSequence) body), StandardCharsets.UTF_8);
            returningBody = body;
        } else if (body instanceof InputStream) {
            body((InputStream) body, UNKNOWN_CONTENT_LENGTH);
        } else if (body == null) {
            immediateBody = Unpooled.EMPTY_BUFFER;
            returningBody = "";
        } else {
            // todo: would be better to write directly to ByteBuf here, but RequestSignerImpl does not yet support
            //  anything but String
            String json;
            try {
                json = MicronautSerdeObjectMapper.getObjectMapper().writeValueAsString(body);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to process JSON body", e);
            }
            immediateBody = ByteBufUtil.encodeString(client.alloc(), CharBuffer.wrap(json), StandardCharsets.UTF_8);
            returningBody = json;
        }
        return this;
    }

    @Override
    public HttpRequest body(InputStream body, long contentLength) {
        immediateBody = null;
        blockingBody = body;
        blockingContentLength = contentLength;
        returningBody = body;
        return this;
    }

    @Override
    public Object body() {
        return returningBody;
    }

    @Override
    public HttpRequest appendPathPart(String encodedPathPart) {
        boolean hasSlashLeft = uri.charAt(uri.length() - 1) == '/';
        boolean hasSlashRight = encodedPathPart.startsWith("/");
        if (hasSlashLeft) {
            if (hasSlashRight) {
                uri.append(encodedPathPart, 1, encodedPathPart.length());
            } else {
                uri.append(encodedPathPart);
            }
        } else {
            if (hasSlashRight) {
                uri.append(encodedPathPart);
            } else {
                uri.append('/').append(encodedPathPart);
            }
        }
        return this;
    }

    @Override
    public HttpRequest query(String name, String value) {
        if (query.length() > 0) {
            query.append('&');
        }
        query.append(name).append('=').append(value);
        return this;
    }

    private String buildUri() {
        int length = uri.length();
        if (query.length() != 0) {
            uri.append('?').append(query);
        }
        String built = uri.toString();
        uri.setLength(length); // remove query again
        return built;
    }

    @Override
    public URI uri() {
        return URI.create(buildUri());
    }

    @Override
    public HttpRequest header(String name, String value) {
        headers.add(name, value);
        return this;
    }

    @Override
    public Map<String, List<String>> headers() {
        return new HeaderMap(headers);
    }

    @Override
    public Object attribute(String name) {
        return attributes.get(name);
    }

    @Override
    public HttpRequest removeAttribute(String name) {
        attributes.remove(name);
        return this;
    }

    @Override
    public HttpRequest attribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    @Override
    public HttpRequest offloadExecutor(Executor offloadExecutor) {
        this.offloadExecutor = offloadExecutor;
        return this;
    }

    @Override
    public HttpRequest copy() {
        return new NettyHttpRequest(this);
    }

    @Override
    public void discard() {
        if (immediateBody != null) {
            immediateBody.release();
        }
    }

    @Override
    public CompletionStage<HttpResponse> execute() {
        if (client.buffered && blockingBody != null) {
            // asynchronously buffer the body, then run execute() again
            return CompletableFuture.runAsync(this::bufferBody, client.blockingIoExecutor)
                    .thenCompose(v -> execute());
        }

        for (RequestInterceptor interceptor : client.requestInterceptors) {
            interceptor.intercept(this);
        }

        io.netty.handler.codec.http.HttpRequest nettyRequest = buildNettyRequest();

        CompletableFuture<HttpResponse> future = new CompletableFuture<>();

        ChannelFuture connectFuture = client.bootstrap.clone()
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        initializeChannel(ch, nettyRequest, future);
                    }
                })
                .connect();
        connectFuture.addListener((ChannelFutureListener) cf -> {
            if (!cf.isSuccess()) {
                future.completeExceptionally(cf.cause());
            }
        });
        // close channel on any failure, including cancellation
        future.exceptionally(t -> {
            connectFuture.channel().close();
            return null;
        });
        return future;
    }

    private void bufferBody() {
        ByteBuf buf = (blockingContentLength == UNKNOWN_CONTENT_LENGTH ?
                client.alloc().buffer() :
                client.alloc().buffer(Math.toIntExact(blockingContentLength)));
        try {
            byte[] arr = new byte[4096];
            while (true) {
                int n = blockingBody.read(arr);
                if (n == -1) {
                    break;
                }
                buf.writeBytes(arr, 0, n);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        blockingBody = null;
        immediateBody = buf;
    }

    private io.netty.handler.codec.http.HttpRequest buildNettyRequest() {
        String uriString = buildUri();

        HttpMethod method;
        switch (this.method) {
            case GET:
                method = HttpMethod.GET;
                break;
            case HEAD:
                method = HttpMethod.HEAD;
                break;
            case DELETE:
                method = HttpMethod.DELETE;
                break;
            case POST:
                method = HttpMethod.POST;
                break;
            case PUT:
                method = HttpMethod.PUT;
                break;
            case PATCH:
                method = HttpMethod.PATCH;
                break;
            default:
                throw new AssertionError(this.method);
        }

        URI uri = URI.create(uriString);
        if (!headers.contains(HttpHeaderNames.HOST)) {
            headers.add(HttpHeaderNames.HOST, uri.getHost());
        }

        String pathAndQuery = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            pathAndQuery = pathAndQuery + "?" + uri.getRawQuery();
        }

        boolean hasTransferHeader = headers.contains(HttpHeaderNames.CONTENT_LENGTH) ||
                headers.contains(HttpHeaderNames.TRANSFER_ENCODING);

        DefaultHttpRequest nettyRequest;
        if (blockingBody != null) {
            if (!hasTransferHeader) {
                if (blockingContentLength == UNKNOWN_CONTENT_LENGTH) {
                    headers.add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
                } else {
                    headers.add(HttpHeaderNames.CONTENT_LENGTH, blockingContentLength);
                }
            }
            nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, pathAndQuery, headers);
        } else {
            ByteBuf body = immediateBody == null ? Unpooled.EMPTY_BUFFER : immediateBody;
            if (!hasTransferHeader) {
                headers.add(HttpHeaderNames.CONTENT_LENGTH, body.readableBytes());
            }
            nettyRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, method, pathAndQuery,
                    body,
                    headers,
                    new DefaultHttpHeaders(true) // trailing header
            );
        }
        return nettyRequest;
    }

    private void initializeChannel(Channel ch, io.netty.handler.codec.http.HttpRequest nettyRequest, CompletableFuture<HttpResponse> future) {
        if (client.sslContext != null) {
            SslHandler sslHandler = client.sslContext.newHandler(ch.alloc(), client.host, client.port);
            // enable host verification
            SSLEngine engine = sslHandler.engine();
            SSLParameters params = engine.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            engine.setSSLParameters(params);

            ch.pipeline().addLast(sslHandler);
        }
        LimitedBufferingBodyHandler limitedBufferingBodyHandler = new LimitedBufferingBodyHandler(4096);
        UndecidedBodyHandler undecidedBodyHandler = new UndecidedBodyHandler();
        ch.pipeline()
                .addLast(new HttpClientCodec())
                .addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        if (msg instanceof io.netty.handler.codec.http.HttpResponse) {
                            future.complete(new NettyHttpResponse((io.netty.handler.codec.http.HttpResponse) msg, limitedBufferingBodyHandler, undecidedBodyHandler, offloadExecutor));
                            ctx.pipeline().remove(this);

                            if (msg instanceof HttpContent) {
                                ctx.fireChannelRead(msg);
                            }
                        } else {
                            ctx.fireChannelRead(msg);
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        future.completeExceptionally(cause);
                        ctx.pipeline().remove(this);
                    }
                })
                .addLast(limitedBufferingBodyHandler)
                .addLast(undecidedBodyHandler)
                .addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        ctx.pipeline().remove(this);
                        ctx.writeAndFlush(nettyRequest, ch.voidPromise());

                        if (blockingBody != null) {
                            ctx.pipeline()
                                    .addLast(new StreamWritingHandler(
                                            blockingBody, client.blockingIoExecutor, new DefaultLastHttpContent()));
                        }

                        ctx.read();
                        super.channelActive(ctx);
                    }
                });
    }
}
