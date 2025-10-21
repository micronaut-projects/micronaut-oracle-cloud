package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.http.client.DefaultHttpClientConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProxySystemPropertiesTest {

    private static final String P_PROXY = "io.micronaut.oci.proxy";
    private static final String P_HOST = "io.micronaut.oci.proxy.host";
    private static final String P_PORT = "io.micronaut.oci.proxy.port";
    private static final String P_USER = "io.micronaut.oci.proxy.username";
    private static final String P_PASS = "io.micronaut.oci.proxy.password";
    private static final String P_TYPE = "io.micronaut.oci.proxy.type";
    private static final String P_NPH  = "io.micronaut.oci.proxy.nonProxyHosts";

    @AfterEach
    void cleanup() {
        clearAll();
    }

    @Test
    void configIsAppliedFromUriProperty() {
        // given
        clearAll();
        System.setProperty(P_PROXY, "http://user:pass@proxy.example:8080");

        DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();

        // when
        NettyHttpClient.applyProxyFromSystemProperties(cfg, LoggerFactory.getLogger(NettyHttpClient.class));

        // then
        assertEquals(Proxy.Type.HTTP, cfg.getProxyType());
        assertNotNull(cfg.getProxyAddress());
        InetSocketAddress addr = (InetSocketAddress) cfg.getProxyAddress().get();
        assertEquals("proxy.example", addr.getHostString());
        assertEquals(8080, addr.getPort());
        assertEquals("user", cfg.getProxyUsername().get());
        assertEquals("pass", cfg.getProxyPassword().get());
    }

    @Test
    void configIsAppliedFromDiscretePropertiesWithSocks() {
        // given
        clearAll();
        System.setProperty(P_HOST, "socks.gateway.internal");
        System.setProperty(P_PORT, "1081");
        System.setProperty(P_TYPE, "socks5");
        System.setProperty(P_USER, "alice");
        System.setProperty(P_PASS, "secret");

        DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();

        // when
        NettyHttpClient.applyProxyFromSystemProperties(cfg, LoggerFactory.getLogger(NettyHttpClient.class));

        // then
        assertEquals(Proxy.Type.SOCKS, cfg.getProxyType());
        InetSocketAddress addr = (InetSocketAddress) cfg.getProxyAddress().get();
        assertEquals("socks.gateway.internal", addr.getHostString());
        assertEquals(1081, addr.getPort());
        assertEquals("alice", cfg.getProxyUsername().get());
        assertEquals("secret", cfg.getProxyPassword().get());
    }

    @Test
    void nonProxyHostsBypassesMatchingHosts() {
        // given
        clearAll();
        System.setProperty(P_HOST, "proxy.acme.local");
        System.setProperty(P_PORT, "3128");
        System.setProperty(P_NPH, "*.internal,localhost,127.0.0.1");

        DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();

        // when
        NettyHttpClient.applyProxyFromSystemProperties(cfg, LoggerFactory.getLogger(NettyHttpClient.class));

        // then
        ProxySelector selector = cfg.getProxySelector().get();
        assertNotNull(selector, "ProxySelector should be configured when nonProxyHosts is set");

        List<Proxy> forInternal = selector.select(URI.create("https://api.internal"));
        Assertions.assertFalse(forInternal.isEmpty());
        assertEquals(Proxy.NO_PROXY, forInternal.get(0), "Hosts matching nonProxyHosts should bypass proxy");

        List<Proxy> forExternal = selector.select(URI.create("https://service.external"));
        Assertions.assertFalse(forExternal.isEmpty());
        Assertions.assertNotEquals(Proxy.NO_PROXY, forExternal.get(0), "Non-matching hosts should use proxy");
    }

    private static void clearAll() {
        System.clearProperty(P_PROXY);
        System.clearProperty(P_HOST);
        System.clearProperty(P_PORT);
        System.clearProperty(P_USER);
        System.clearProperty(P_PASS);
        System.clearProperty(P_TYPE);
        System.clearProperty(P_NPH);
    }
}
