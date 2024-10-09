package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Deprecated
class StreamWritingHandlerTest {
    @Test
    public void test() {
        EmbeddedChannel channel = new EmbeddedChannel();
        Object terminationMessage = new Object();
        channel.pipeline().addLast(new StreamWritingHandler(
                new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)),
                channel.eventLoop(),
                terminationMessage
        ));

        channel.runPendingTasks();
        channel.checkException();

        assertEquals("foo", channel.<ByteBuf>readOutbound().toString(StandardCharsets.UTF_8));
        assertSame(terminationMessage, channel.readOutbound());
    }

    @Test
    public void chunked() {
        EmbeddedChannel channel = new EmbeddedChannel();
        Object terminationMessage = new Object();
        channel.pipeline().addLast(new StreamWritingHandler(
                ChunkedInputStream.fromUtf8("fo", "o"),
                channel.eventLoop(),
                terminationMessage
        ));

        channel.runPendingTasks();
        channel.checkException();

        assertEquals("fo", channel.<ByteBuf>readOutbound().toString(StandardCharsets.UTF_8));
        assertEquals("o", channel.<ByteBuf>readOutbound().toString(StandardCharsets.UTF_8));
        assertSame(terminationMessage, channel.readOutbound());
    }

    @SuppressWarnings("resource")
    private static class ChunkedInputStream extends InputStream {
        private final Queue<InputStream> delegates;

        private ChunkedInputStream(Queue<InputStream> delegates) {
            this.delegates = delegates;
        }

        static ChunkedInputStream fromUtf8(String... strings) {
            Queue<InputStream> queue = new ArrayDeque<>(strings.length);
            for (String string : strings) {
                queue.add(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
            }
            return new ChunkedInputStream(queue);
        }

        @Override
        public int read() throws IOException {
            while (true) {
                InputStream first = delegates.peek();
                if (first == null) {
                    return -1;
                } else {
                    int read = first.read();
                    if (read == -1) {
                        first.close();
                        delegates.poll();
                        // retry
                    } else {
                        return read;
                    }
                }
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            while (true) {
                InputStream first = delegates.peek();
                if (first == null) {
                    return -1;
                } else {
                    int read = first.read(b, off, len);
                    if (read == -1) {
                        first.close();
                        delegates.poll();
                        // retry
                    } else {
                        return read;
                    }
                }
            }
        }
    }
}
