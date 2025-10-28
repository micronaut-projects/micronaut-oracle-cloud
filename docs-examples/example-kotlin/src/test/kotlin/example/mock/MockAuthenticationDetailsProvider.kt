package example.mock

import com.oracle.bmc.auth.AuthCachingPolicy
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton
import java.io.InputStream

@AuthCachingPolicy(cacheKeyId = false, cachePrivateKey = false)
@Singleton
@Replaces(AuthenticationDetailsProvider::class)
@Primary
class MockAuthenticationDetailsProvider : AuthenticationDetailsProvider {
    override fun getKeyId(): String? {
        return null
    }

    override fun getPrivateKey(): InputStream? {
        return null
    }

    override fun getPassPhrase(): String? {
        return null
    }

    override fun getPassphraseCharacters(): CharArray? {
        return null
    }

    override fun getFingerprint(): String {
        return ""
    }

    override fun getTenantId(): String {
        return ""
    }

    override fun getUserId(): String {
        return ""
    }
}
