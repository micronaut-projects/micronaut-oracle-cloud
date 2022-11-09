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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fnproject.fn.api.FnConfiguration;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jni.JNIRuntimeAccess;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.util.ArrayUtils;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
                    ReflectionUtils.findMethodsByName(c, s[1])
                            .forEach(method -> {
                                RuntimeReflection.register(method);
                                final Class<?> returnType = method.getReturnType();
                                if (returnType != void.class) {
                                    if (!ClassUtils.isJavaBasicType(returnType)) {
                                        registerForReflection(returnType);
                                    }
                                }
                                final Class<?>[] parameterTypes = method.getParameterTypes();
                                for (Class<?> parameterType : parameterTypes) {
                                    if (!ClassUtils.isJavaBasicType(parameterType)) {
                                        registerForReflection(parameterType);
                                    }
                                }
                            });
                    Method[] declaredMethods = c.getDeclaredMethods();
                    for (Method declaredMethod : declaredMethods) {
                        if (declaredMethod.getAnnotation(FnConfiguration.class) != null) {
                            RuntimeReflection.register(declaredMethod);
                        }
                    }
                }
            }
        }
        Class<?> clbClass;
        try {
            clbClass = Class.forName("org.glassfish.jersey.client.JerseyClientBuilder");
        } catch (ReflectiveOperationException e) {
            clbClass = null;
        }
        if (clbClass != null) {
            registerIfNecessary(clbClass);
        }
    }

    private void registerForReflection(Class<?> type) {
        if (type.getAnnotation(Introspected.class) != null) {
            // no need for reflection
            return;
        }

        checkDeserialize(type);
        final JsonTypeInfo ti = type.getAnnotation(JsonTypeInfo.class);
        if (ti != null) {
            final Class<?> di = ti.defaultImpl();
            if (di != JsonTypeInfo.class) {
                registerIfNecessary(di);
            }
        }

        final JsonSubTypes subTypes = type.getAnnotation(JsonSubTypes.class);
        if (subTypes != null) {
            final JsonSubTypes.Type[] types = subTypes.value();
            if (ArrayUtils.isNotEmpty(types)) {
                for (JsonSubTypes.Type t : types) {
                    final Class<?> v = t.value();
                    registerIfNecessary(v);
                }
            }
        }
    }

    private static void checkDeserialize(AnnotatedElement type) {
        JsonDeserialize deser = type.getAnnotation(JsonDeserialize.class);
        if (deser != null) {
            registerIfNecessary(deser.builder());
            registerIfNecessary(deser.as());
            registerIfNecessary(deser.contentAs());
            registerIfNecessary(deser.keyAs());
            registerIfNecessary(deser.using());
        }
    }

    private static void registerIfNecessary(Class<?> t) {
        if (t != Object.class && t != Void.class && !Modifier.isAbstract(t.getModifiers())) {
            registerAllForRuntimeReflectionAndReflectiveInstantiation(t);
        }
    }

    private static void registerAllForRuntimeReflectionAndReflectiveInstantiation(Class<?> clazz) {
        registerForRuntimeReflection(clazz);
        registerForReflectiveInstantiation(clazz);
        registerFieldsForRuntimeReflection(clazz);
        registerMethodsForRuntimeReflection(clazz);
        registerConstructorsForRuntimeReflection(clazz);
    }

    private static void registerAllForRuntimeReflection(Class<?> clazz) {
        registerForRuntimeReflection(clazz);
        registerFieldsForRuntimeReflection(clazz);
        registerMethodsForRuntimeReflection(clazz);
        registerConstructorsForRuntimeReflection(clazz);
    }

    private static void registerFieldsAndMethodsWithReflectiveAccess(Class<?> clazz) {
        registerForRuntimeReflectionAndReflectiveInstantiation(clazz);
        registerMethodsForRuntimeReflection(clazz);
        registerFieldsForRuntimeReflection(clazz);
    }

    private static void registerForRuntimeReflection(Class<?> clazz) {
        RuntimeReflection.register(clazz);
    }

    private static void registerForReflectiveInstantiation(Class<?> clazz) {
        RuntimeReflection.registerForReflectiveInstantiation(clazz);
    }

    private static void registerForRuntimeReflectionAndReflectiveInstantiation(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        RuntimeReflection.registerForReflectiveInstantiation(clazz);
    }

    private static void registerMethodsForRuntimeReflection(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            checkDeserialize(method);
            RuntimeReflection.register(method);
        }
    }

    private static void registerFieldsForRuntimeReflection(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            checkDeserialize(field);
            RuntimeReflection.register(field);
        }
    }

    private static void registerConstructorsForRuntimeReflection(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            RuntimeReflection.register(constructor);
        }
    }
}
