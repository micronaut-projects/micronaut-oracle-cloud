/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.oraclecloud.oke.workload.identity;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.client.HttpVersionSelection;
import io.micronaut.http.client.netty.ssl.ClientSslBuilder;
import io.micronaut.http.netty.NettyTlsUtils;
import io.micronaut.http.ssl.ClientAuthentication;
import io.micronaut.http.ssl.SslBuilder;
import io.micronaut.http.ssl.SslConfiguration;
import io.micronaut.http.ssl.SslConfigurationException;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.Optional;

/**
 * The Netty implementation of {@link SslBuilder} that generates an {@link SslContext} to create a client that
 * supports SSL.<br>
 * This class is not final, so you can extend and replace it to implement alternate mechanisms for loading the
 * key and trust stores.
 */
final class OkeNettyClientSslBuilder implements ClientSslBuilder {
    /**
     * @param resourceResolver The resource resolver
     */
    private final TrustManagerFactory trustManagerFactory;

    private final KeyStore keyStore;

    public OkeNettyClientSslBuilder(TrustManagerFactory trustManagerFactory, KeyStore keyStore) {
        this.trustManagerFactory = trustManagerFactory;
        this.keyStore = keyStore;
    }

    @NonNull
    @Override
    public SslContext build(SslConfiguration ssl, HttpVersionSelection versionSelection) {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());

            keyManagerFactory.init(keyStore, null);

            SslContextBuilder sslBuilder = SslContextBuilder
                .forClient()
                .keyManager(keyManagerFactory)
                .trustManager(trustManagerFactory)
                .sslProvider(NettyTlsUtils.sslProvider());
            Optional<String[]> protocols = ssl.getProtocols();
            if (protocols.isPresent()) {
                sslBuilder.protocols(protocols.get());
            }
            Optional<String[]> ciphers = ssl.getCiphers();
            if (ciphers.isPresent()) {
                sslBuilder = sslBuilder.ciphers(Arrays.asList(ciphers.get()));
            } else if (versionSelection.isHttp2CipherSuites()) {
                sslBuilder.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE);
            }
            Optional<ClientAuthentication> clientAuthentication = ssl.getClientAuthentication();
            if (clientAuthentication.isPresent()) {
                ClientAuthentication clientAuth = clientAuthentication.get();
                if (clientAuth == ClientAuthentication.NEED) {
                    sslBuilder = sslBuilder.clientAuth(ClientAuth.REQUIRE);
                } else if (clientAuth == ClientAuthentication.WANT) {
                    sslBuilder = sslBuilder.clientAuth(ClientAuth.OPTIONAL);
                }
            }
            if (versionSelection.isAlpn()) {
                SslProvider provider = SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
                sslBuilder.sslProvider(provider);
                sslBuilder.applicationProtocolConfig(new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    versionSelection.getAlpnSupportedProtocols()
                ));
            }

            try {
                return sslBuilder.build();
            } catch (SSLException ex) {
                throw new SslConfigurationException("An error occurred while setting up SSL", ex);
            }

        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
