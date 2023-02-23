package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

class UndecidedBodyHandlerTest {
    @Test
    public void fullyBufferedStream() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        UndecidedBodyHandler handler = new UndecidedBodyHandler(channel.alloc());
        channel.pipeline().addLast(handler);

        channel.writeInbound(new DefaultLastHttpContent(Unpooled.wrappedBuffer("foo".getBytes(StandardCharsets.UTF_8))));

        CompletableFuture<InputStream> stream = handler.asInputStream();
        channel.runPendingTasks();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream.get(), StandardCharsets.UTF_8))) {
            Assertions.assertEquals("foo", reader.readLine());
        }
    }

    @Test
    public void fullyBufferedFuture() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        UndecidedBodyHandler handler = new UndecidedBodyHandler(channel.alloc());
        channel.pipeline().addLast(handler);

        channel.writeInbound(new DefaultLastHttpContent(Unpooled.wrappedBuffer("foo".getBytes(StandardCharsets.UTF_8))));

        CompletableFuture<ByteBuf> future = handler.asBuffer();
        channel.runPendingTasks();
        Assertions.assertTrue(future.isDone());
        Assertions.assertEquals(Unpooled.wrappedBuffer("foo".getBytes(StandardCharsets.UTF_8)), future.get());
    }

    @Test
    public void closeImmediatelyAsBuffer() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        UndecidedBodyHandler handler = new UndecidedBodyHandler(channel.alloc());
        channel.pipeline().addLast(handler);
        channel.writeInbound(new DefaultLastHttpContent());

        CompletableFuture<?> future = handler.asBuffer();
        try {
            channel.pipeline().remove(handler);
        } catch (NoSuchElementException ignored) {
            // already removed by asBuffer
        }
        channel.runPendingTasks();
        Assertions.assertTrue(future.isDone());
    }

    @Test
    public void closeImmediatelyAsStream() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        UndecidedBodyHandler handler = new UndecidedBodyHandler(channel.alloc());
        channel.pipeline().addLast(handler);
        channel.writeInbound(new DefaultLastHttpContent());

        CompletableFuture<?> future = handler.asInputStream();
        try {
            channel.pipeline().remove(handler);
        } catch (NoSuchElementException ignored) {
            // already removed by asInputStream
        }
        channel.runPendingTasks();
        Assertions.assertTrue(future.isDone());
    }
}