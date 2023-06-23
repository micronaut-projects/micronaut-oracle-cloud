package io.micronaut.oraclecloud.httpclient.netty;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

@SuppressWarnings({"RedundantCollectionOperation", "MismatchedQueryAndUpdateOfCollection"})
class HeaderMapTest {
    @Test
    public void containsCaseInsensitive() {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add("Foo", "bar");

        HeaderMap map = new HeaderMap(headers);
        Assertions.assertTrue(map.containsKey("foo"));
        Assertions.assertTrue(map.containsKey("FOO"));
        Assertions.assertTrue(map.containsKey("Foo"));
        Assertions.assertTrue(map.keySet().contains("foo"));
        Assertions.assertTrue(map.keySet().contains("FOO"));
        Assertions.assertTrue(map.keySet().contains("Foo"));

        List<String> foo = map.remove("Foo");

        Assertions.assertEquals(foo.size(), 1);
        Assertions.assertEquals(foo.get(0), "bar");
        Assertions.assertFalse(map.containsKey("foo"));
        Assertions.assertFalse(map.containsKey("FOO"));
        Assertions.assertFalse(map.containsKey("Foo"));
        Assertions.assertFalse(map.keySet().contains("foo"));
        Assertions.assertFalse(map.keySet().contains("FOO"));
        Assertions.assertFalse(map.keySet().contains("Foo"));
    }
}
