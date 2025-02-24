package io.micronaut.oraclecloud.httpclient.netty;

import com.oracle.bmc.auth.AuthCachingPolicy;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.io.InputStream;

@AuthCachingPolicy(cacheKeyId = false, cachePrivateKey = false)
@Singleton
@Replaces(AuthenticationDetailsProvider.class)
@Primary
@Requires(missingProperty = "vault.secrets.compartment.ocid")
@Requires(missingProperty = "vault.ocid")
@Requires(missingProperty = "monitoring.compartment.ocid")
public class MockAuthenticationDetailsProvider implements AuthenticationDetailsProvider {

    @Override
    public String getKeyId() {
        return null;
    }

    @Override
    public InputStream getPrivateKey() {
        return null;
    }

    @Override
    public String getPassPhrase() {
        return null;
    }

    @Override
    public char[] getPassphraseCharacters() {
        return null;
    }

    @Override
    public String getFingerprint() {
        return "";
    }

    @Override
    public String getTenantId() {
        return "";
    }

    @Override
    public String getUserId() {
        return "";
    }
}
