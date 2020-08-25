package io.micronaut.oci.clients;

import com.oracle.svm.core.annotate.AutomaticFeature;
import io.micronaut.core.annotation.Internal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import java.security.Security;

/**
 * Configures bouncy castle.
 */
@AutomaticFeature
@Internal
final class BouncyCastleFeature implements Feature {

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        RuntimeClassInitialization.initializeAtBuildTime("org.bouncycastle");
        RuntimeClassInitializationSupport classInitSupport = ImageSingletons.lookup(RuntimeClassInitializationSupport.class);
        classInitSupport.rerunInitialization(
                "org.bouncycastle.jcajce.provider.drbg.DRBG$Default",
                "See https://github.com/micronaut-projects/micronaut-oracle-cloud/pull/17#discussion_r472955378"
        );
        classInitSupport.rerunInitialization(
                "org.bouncycastle.jcajce.provider.drbg.DRBG$NonceAndIV",
                "See https://github.com/micronaut-projects/micronaut-oracle-cloud/pull/17#discussion_r472955378"
        );
        Security.addProvider(new BouncyCastleProvider());
    }
}

