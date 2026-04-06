package io.micronaut.oraclecloud.discovery.vault

import com.oracle.bmc.auth.AuthCachingPolicy
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import io.micronaut.context.annotation.BootstrapContextCompatible
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@AuthCachingPolicy(cacheKeyId = false, cachePrivateKey = false)
@BootstrapContextCompatible
@Singleton
@Replaces(ConfigFileAuthenticationDetailsProvider)
@Primary
@Requires(missingProperty = "vault.ocid")
class MockAuthenticationDetailsProvider implements BasicAuthenticationDetailsProvider {

    @Override
    String getPassPhrase() {
        null
    }

    @Override
    char[] getPassphraseCharacters() {
        new char[0]
    }

    @Override
    String getKeyId() {
        'test-key-id'
    }

    @Override
    InputStream getPrivateKey() {
        new ByteArrayInputStream('test-private-key'.bytes)
    }
}
