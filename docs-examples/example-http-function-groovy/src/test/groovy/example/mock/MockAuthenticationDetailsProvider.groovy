package example.mock

import com.oracle.bmc.auth.AuthCachingPolicy
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Replaces

import jakarta.inject.Singleton

@CompileStatic
@AuthCachingPolicy(cacheKeyId = false, cachePrivateKey = false)
@Singleton
@Replaces(AuthenticationDetailsProvider)
class MockAuthenticationDetailsProvider implements AuthenticationDetailsProvider {
    String keyId
    InputStream privateKey
    String passPhrase
    char[] passphraseCharacters
    String fingerprint
    String tenantId
    String userId
}
