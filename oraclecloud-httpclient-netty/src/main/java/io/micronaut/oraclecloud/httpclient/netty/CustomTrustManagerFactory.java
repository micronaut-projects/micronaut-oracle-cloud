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

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Wrapper around {@link TrustManagerFactory} that falls back to a {@link HostnameVerifier} if the normal cert check
 * fails.
 */
final class CustomTrustManagerFactory extends SimpleTrustManagerFactory {
    private final TrustManagerFactory delegate;
    private final HostnameVerifier fallback;

    CustomTrustManagerFactory(TrustManagerFactory delegate, HostnameVerifier fallback) {
        this.delegate = delegate;
        this.fallback = fallback;
    }

    @Override
    protected void engineInit(KeyStore keyStore) throws Exception {
        delegate.init(keyStore);
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
        delegate.init(managerFactoryParameters);
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return Arrays.stream(delegate.getTrustManagers())
                .map(tm -> new CustomTrustManager((X509ExtendedTrustManager) tm, fallback))
                .toArray(TrustManager[]::new);
    }

    private static final class CustomTrustManager extends X509ExtendedTrustManager {
        private final X509ExtendedTrustManager delegate;
        private final HostnameVerifier fallback;

        CustomTrustManager(X509ExtendedTrustManager delegate, HostnameVerifier fallback) {
            this.delegate = delegate;
            this.fallback = fallback;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            try {
                delegate.checkServerTrusted(chain, authType, engine);
            } catch (CertificateException e) {
                // note: getPeerHost javadoc says the value cannot be trusted. However, this value is also used by
                // OpenJDK X509TrustManagerImpl. Debugging also shows this value does not come from the handshake, it's
                // the value we pass to sslContext.newHandler. So it should be fine.
                String host = engine.getPeerHost();
                // wrap the SSLSession to return the right certificate chain. The chain is only set after
                // checkServerTrusted succeeds, so we need to do extra work to provide it to the hostname verifier.
                CustomSslSession session = new CustomSslSession((ExtendedSSLSession) engine.getHandshakeSession(), chain);
                if (!fallback.verify(host, session)) {
                    throw e;
                }
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            throw new UnsupportedOperationException();
        }
    }

    private static class CustomSslSession extends ExtendedSSLSession {
        private final ExtendedSSLSession delegate;
        private final X509Certificate[] peerCertificates;

        CustomSslSession(ExtendedSSLSession delegate, X509Certificate[] peerCertificates) {
            this.delegate = delegate;
            this.peerCertificates = peerCertificates;
        }

        @Override
        public String[] getLocalSupportedSignatureAlgorithms() {
            return delegate.getLocalSupportedSignatureAlgorithms();
        }

        @Override
        public String[] getPeerSupportedSignatureAlgorithms() {
            return delegate.getPeerSupportedSignatureAlgorithms();
        }

        @Override
        public List<SNIServerName> getRequestedServerNames() {
            return delegate.getRequestedServerNames();
        }

        @Override
        public byte[] getId() {
            return delegate.getId();
        }

        @Override
        public SSLSessionContext getSessionContext() {
            return delegate.getSessionContext();
        }

        @Override
        public long getCreationTime() {
            return delegate.getCreationTime();
        }

        @Override
        public long getLastAccessedTime() {
            return delegate.getLastAccessedTime();
        }

        @Override
        public void invalidate() {
            delegate.invalidate();
        }

        @Override
        public boolean isValid() {
            return delegate.isValid();
        }

        @Override
        public void putValue(String name, Object value) {
            delegate.putValue(name, value);
        }

        @Override
        public Object getValue(String name) {
            return delegate.getValue(name);
        }

        @Override
        public void removeValue(String name) {
            delegate.removeValue(name);
        }

        @Override
        public String[] getValueNames() {
            return delegate.getValueNames();
        }

        @Override
        public Certificate[] getPeerCertificates() {
            return peerCertificates;
        }

        @Override
        public Certificate[] getLocalCertificates() {
            return delegate.getLocalCertificates();
        }

        @Override
        public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
            return delegate.getPeerPrincipal();
        }

        @Override
        public Principal getLocalPrincipal() {
            return delegate.getLocalPrincipal();
        }

        @Override
        public String getCipherSuite() {
            return delegate.getCipherSuite();
        }

        @Override
        public String getProtocol() {
            return delegate.getProtocol();
        }

        @Override
        public String getPeerHost() {
            return delegate.getPeerHost();
        }

        @Override
        public int getPeerPort() {
            return delegate.getPeerPort();
        }

        @Override
        public int getPacketBufferSize() {
            return delegate.getPacketBufferSize();
        }

        @Override
        public int getApplicationBufferSize() {
            return delegate.getApplicationBufferSize();
        }
    }
}
