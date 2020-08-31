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
package io.micronaut.oraclecloud.clients;

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

