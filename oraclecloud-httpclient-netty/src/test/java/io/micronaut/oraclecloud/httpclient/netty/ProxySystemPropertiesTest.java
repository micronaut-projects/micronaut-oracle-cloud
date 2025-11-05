package io.micronaut.oraclecloud.httpclient.netty;

import io.micronaut.core.util.StringUtils;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxySystemPropertiesTest {

    private static final String P_PROXY = "io.micronaut.oci.proxy";
    private static final String P_HOST = "io.micronaut.oci.proxy.host";
    private static final String P_PORT = "io.micronaut.oci.proxy.port";
    private static final String P_USER = "io.micronaut.oci.proxy.username";
    private static final String P_PASS = "io.micronaut.oci.proxy.password";
    private static final String P_TYPE = "io.micronaut.oci.proxy.type";
    private static final String P_NPH  = "io.micronaut.oci.proxy.nonProxyHosts";

    @Test
    void configIsAppliedFromUriProperty() {
        SystemProperty systemProperty = new SystemProperty(P_PROXY, "http://user:pass@proxy.example:8080");
        DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();

        // when
        NettyHttpClient.applyProxyFromSystemProperties(cfg, LoggerFactory.getLogger(NettyHttpClient.class));

        // then
        assertEquals(Proxy.Type.HTTP, cfg.getProxyType());
        assertNotNull(cfg.getProxyAddress());
        assertTrue(cfg.getProxyAddress().isPresent());
        InetSocketAddress addr = (InetSocketAddress) cfg.getProxyAddress().get();
        assertEquals("proxy.example", addr.getHostString());
        assertEquals(8080, addr.getPort());
        assertTrue(cfg.getProxyUsername().isPresent());
        assertEquals("user", cfg.getProxyUsername().get());
        assertTrue(cfg.getProxyPassword().isPresent());
        assertEquals("pass", cfg.getProxyPassword().get());
        systemProperty.restore();
    }

    @Test
    void configIsAppliedFromDiscretePropertiesWithSocks() {
        // given:
        SystemProperty systemPropertyHost = new SystemProperty(P_HOST, "socks.gateway.internal");
        SystemProperty systemPropertyPort = new SystemProperty(P_PORT, "1081");
        SystemProperty systemPropertyType = new SystemProperty(P_TYPE, "socks5");
        SystemProperty systemPropertyUser = new SystemProperty(P_USER, "alice");
        SystemProperty systemPropertyPass = new SystemProperty(P_PASS, "secret");

        DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();

        // when
        NettyHttpClient.applyProxyFromSystemProperties(cfg, LoggerFactory.getLogger(NettyHttpClient.class));

        // then
        assertEquals(Proxy.Type.SOCKS, cfg.getProxyType());
        assertTrue(cfg.getProxyAddress().isPresent());
        InetSocketAddress addr = (InetSocketAddress) cfg.getProxyAddress().get();
        assertEquals("socks.gateway.internal", addr.getHostString());
        assertEquals(1081, addr.getPort());
        assertTrue(cfg.getProxyUsername().isPresent());
        assertEquals("alice", cfg.getProxyUsername().get());
        assertTrue(cfg.getProxyPassword().isPresent());
        assertEquals("secret", cfg.getProxyPassword().get());

        // close
        systemPropertyHost.restore();
        systemPropertyPort.restore();
        systemPropertyType.restore();
        systemPropertyUser.restore();
        systemPropertyPass.restore();
    }

    @Test
    void nonProxyHostsBypassesMatchingHosts() {
        // given
        SystemProperty systemPropertyHost = new SystemProperty(P_HOST, "proxy.acme.local");
        SystemProperty systemPropertyPort = new SystemProperty(P_PORT, "3128");
        SystemProperty systemPropertyNph = new SystemProperty(P_NPH, "*.internal,localhost,127.0.0.1");

        DefaultHttpClientConfiguration cfg = new DefaultHttpClientConfiguration();

        // when
        NettyHttpClient.applyProxyFromSystemProperties(cfg, LoggerFactory.getLogger(NettyHttpClient.class));

        // then
        assertTrue(cfg.getProxySelector().isPresent());

        // when
        ProxySelector selector = cfg.getProxySelector().get();

        // then
        assertNotNull(selector, "ProxySelector should be configured when nonProxyHosts is set");

        // when
        List<Proxy> forInternal = selector.select(URI.create("https://api.internal"));

        // then
        assertFalse(forInternal.isEmpty());
        assertEquals(Proxy.NO_PROXY, forInternal.get(0), "Hosts matching nonProxyHosts should bypass proxy");

        // when
        List<Proxy> forExternal = selector.select(URI.create("https://service.external"));

        // then
        assertFalse(forExternal.isEmpty());
        assertNotEquals(Proxy.NO_PROXY, forExternal.get(0), "Non-matching hosts should use proxy");

        // close
        systemPropertyHost.restore();
        systemPropertyPort.restore();
        systemPropertyNph.restore();
    }

    static class SystemProperty {
        private final String name;
        private final String previousValue;

        SystemProperty(String name, String value) {
            this.previousValue = System.getProperty(name);
            this.name = name;
            System.setProperty(name, value);
        }

        public void restore() {
            if (StringUtils.isNotEmpty(previousValue)) {
                System.setProperty(name, previousValue);
            } else {
                System.clearProperty(name);
            }
        }
    }
}
