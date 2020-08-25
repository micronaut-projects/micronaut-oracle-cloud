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
