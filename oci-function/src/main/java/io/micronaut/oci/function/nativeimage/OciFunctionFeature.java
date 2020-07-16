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
package io.micronaut.oci.function.nativeimage;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jni.JNIRuntimeAccess;
import io.micronaut.core.annotation.Internal;
import org.graalvm.nativeimage.hosted.Feature;

/**
 * An automatic feature for native functions.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@SuppressWarnings("unused")
@AutomaticFeature
@Internal
final class OciFunctionFeature implements Feature {

    private static final String UNIX_SOCKET_NATIVE = "com.fnproject.fn.runtime.ntv.UnixSocketNative";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> t = access.findClassByName(UNIX_SOCKET_NATIVE);
        if (t != null) {
            JNIRuntimeAccess.register(t);
            JNIRuntimeAccess.register(t.getDeclaredMethods());
        }
    }
}
