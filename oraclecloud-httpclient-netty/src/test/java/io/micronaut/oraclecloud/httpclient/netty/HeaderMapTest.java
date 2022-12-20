package io.micronaut.oraclecloud.httpclient.netty;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    }
}