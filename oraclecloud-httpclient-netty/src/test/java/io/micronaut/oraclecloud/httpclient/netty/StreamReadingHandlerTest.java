package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class StreamReadingHandlerTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        executor = null;
    }

    @Test
    public void simple() throws Throwable {
        EmbeddedChannel channel = new EmbeddedChannel();
        StreamReadingHandler handler = new StreamReadingHandler();
        channel.pipeline().addLast(handler);
        InputStream stream = handler.getInputStream();
        byte[] buffer = new byte[1024];

        channel.writeInbound(Unpooled.wrappedBuffer("foo".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals(3, stream.read(buffer));
        Assertions.assertEquals("foo", new String(buffer, 0, 3, StandardCharsets.UTF_8));

        channel.writeInbound(Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals(3, stream.read(buffer));
        Assertions.assertEquals("bar", new String(buffer, 0, 3, StandardCharsets.UTF_8));

        channel.pipeline().remove(handler);
        Assertions.assertEquals(-1, stream.read(buffer));
    }

    @Test
    public void blocking() throws Throwable {
        EmbeddedChannel channel = new EmbeddedChannel();
        StreamReadingHandler handler = new StreamReadingHandler();
        channel.pipeline().addLast(handler);
        InputStream stream = handler.getInputStream();

        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

        Future<?> future = executor.submit(() -> {
            char[] buffer = new char[1024];
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            while (true) {
                int n = reader.read(buffer);
                if (n == -1) {
                    break;
                }
                queue.add(new String(buffer, 0, n));
            }
            return null;
        });

        channel.writeInbound(Unpooled.wrappedBuffer("foo".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals("foo", queue.take());
        channel.writeInbound(Unpooled.wrappedBuffer("bar".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals("bar", queue.take());

        channel.pipeline().remove(handler);

        future.get();
    }

    @Test
    public void fullyBuffered() throws Throwable {
        EmbeddedChannel channel = new EmbeddedChannel();
        StreamReadingHandler handler = new StreamReadingHandler();
        channel.pipeline().addLast(handler);
        InputStream stream = handler.getInputStream();
        byte[] buffer = new byte[1024];

        channel.writeInbound(Unpooled.wrappedBuffer("foo".getBytes(StandardCharsets.UTF_8)));
        channel.pipeline().remove(handler);

        Assertions.assertEquals(3, stream.read(buffer));
        Assertions.assertEquals("foo", new String(buffer, 0, 3, StandardCharsets.UTF_8));
        Assertions.assertEquals(-1, stream.read(buffer));
    }

    @Test
    public void failure() throws Throwable {
        EmbeddedChannel channel = new EmbeddedChannel();
        StreamReadingHandler handler = new StreamReadingHandler();
        channel.pipeline().addLast(handler);
        InputStream stream = handler.getInputStream();

        channel.pipeline().fireExceptionCaught(new RuntimeException("foo"));
        channel.pipeline().fireExceptionCaught(new RuntimeException("bar"));

        try {
            stream.read();
            Assertions.fail();
        } catch (IOException e) {
            Assertions.assertEquals("foo", e.getCause().getMessage());
        }

        try {
            channel.checkException();
        } catch (Exception e) {
            Assertions.assertEquals("bar", e.getMessage());
        }
    }
}