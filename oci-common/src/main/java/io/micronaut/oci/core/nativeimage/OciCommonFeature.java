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
package io.micronaut.oci.core.nativeimage;

import com.oracle.svm.core.annotate.AutomaticFeature;
import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

import java.util.Arrays;

/**
 * OCI SDK needs Sun XML classes initialized at build time.
 *
 * @author gkrocher
 * @since 1.0.0
 */
@Internal
@AutomaticFeature
final class OciCommonFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Arrays.asList(
                "java.awt",
                "jdk.xml",
                "com.sun.org.apache.xerces",
                "com.sun.xml",
                "sun.awt",
                "sun.java2d").forEach(RuntimeClassInitialization::initializeAtBuildTime);

    }

}
