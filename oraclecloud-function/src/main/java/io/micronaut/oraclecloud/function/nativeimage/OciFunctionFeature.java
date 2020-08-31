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
package io.micronaut.oraclecloud.function.nativeimage;

import com.fnproject.fn.api.FnConfiguration;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jni.JNIRuntimeAccess;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.reflect.ReflectionUtils;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Method;

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
    private static final String FN_HANDLER = "fn.handler";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> t = access.findClassByName(UNIX_SOCKET_NATIVE);
        if (t != null) {
            JNIRuntimeAccess.register(t);
            JNIRuntimeAccess.register(t.getDeclaredMethods());
            RuntimeClassInitialization.initializeAtRunTime(t);
        }

        String handler = System.getProperty(FN_HANDLER);
        if (handler != null) {
            String[] s = handler.split("::");
            if (s.length == 2) {
                Class<?> c = access.findClassByName(s[0]);
                if (c != null) {

                    RuntimeReflection.register(c);
                    RuntimeReflection.registerForReflectiveInstantiation(c);
                    ReflectionUtils.findMethod(c, s[1])
                            .ifPresent(RuntimeReflection::register);
                    Method[] declaredMethods = c.getDeclaredMethods();
                    for (Method declaredMethod : declaredMethods) {
                        if (declaredMethod.getAnnotation(FnConfiguration.class) != null) {
                            RuntimeReflection.register(declaredMethod);
                        }
                    }
                }
            }
        }
    }
}
