package example.mock

import com.oracle.bmc.auth.AuthCachingPolicy
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces

import jakarta.inject.Singleton

@CompileStatic
@AuthCachingPolicy(cacheKeyId = false, cachePrivateKey = false)
@Singleton
@Replaces(AuthenticationDetailsProvider)
@Primary
class MockAuthenticationDetailsProvider implements AuthenticationDetailsProvider {
    String keyId
    InputStream privateKey
    String passPhrase
    char[] passphraseCharacters
    String fingerprint
    String tenantId
    String userId
}
