package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpHeaders;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"RedundantCollectionOperation", "MismatchedQueryAndUpdateOfCollection"})
class HeaderMapTest {
    @Test
    public void containsCaseInsensitive() {
        MutableHttpHeaders headers = HttpRequest.GET("").getHeaders();
        headers.add("Foo", "bar");

        MicronautHeaderMap map = new MicronautHeaderMap(headers);
        assertTrue(map.containsKey("foo"));
        assertTrue(map.containsKey("FOO"));
        assertTrue(map.containsKey("Foo"));
        assertTrue(map.keySet().contains("foo"));
        assertTrue(map.keySet().contains("FOO"));
        assertTrue(map.keySet().contains("Foo"));

        List<String> foo = map.remove("Foo");

        assertEquals(1, foo.size());
        assertEquals("bar", foo.getFirst());
        assertFalse(map.containsKey("foo"));
        assertFalse(map.containsKey("FOO"));
        assertFalse(map.containsKey("Foo"));
        assertFalse(map.keySet().contains("foo"));
        assertFalse(map.keySet().contains("FOO"));
        assertFalse(map.keySet().contains("Foo"));

        assertNull(map.remove("Foo"));
    }
}
