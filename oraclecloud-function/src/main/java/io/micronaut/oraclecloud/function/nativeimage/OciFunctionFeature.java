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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.util.ArrayUtils;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
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
@Internal
final class OciFunctionFeature implements Feature {

    private static final String UNIX_SOCKET_NATIVE = "com.fnproject.fn.runtime.ntv.UnixSocketNative";
    private static final String FN_HANDLER = "fn.handler";
    private static final String JACKSON_2_JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo";
    private static final String JACKSON_2_JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes";
    private static final String JACKSON_2_JSON_DESERIALIZE = "com.fasterxml.jackson.databind.annotation.JsonDeserialize";
    private static final String JACKSON_3_JSON_TYPE_INFO = "tools.jackson.annotation.JsonTypeInfo";
    private static final String JACKSON_3_JSON_SUB_TYPES = "tools.jackson.annotation.JsonSubTypes";
    private static final String JACKSON_3_JSON_DESERIALIZE = "tools.jackson.databind.annotation.JsonDeserialize";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> t = access.findClassByName(UNIX_SOCKET_NATIVE);
        if (t != null) {
            try {
                // graalvm 22.3
                // need to use reflection because 22.3 is java11+ only, can't compile against that
                Class.forName("org.graalvm.nativeimage.hosted.RuntimeJNIAccess")
                        .getMethod("register", Class[].class)
                        .invoke(null, (Object) new Class[]{t});
                Class.forName("org.graalvm.nativeimage.hosted.RuntimeJNIAccess")
                        .getMethod("register", Executable[].class)
                        .invoke(null, (Object) t.getDeclaredMethods());
            } catch (ReflectiveOperationException e) {
                // fall back to old api
                RuntimeJNIAccess.register(t);
                RuntimeJNIAccess.register(t.getDeclaredMethods());
            }
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
        Class<?> clbClass = access.findClassByName("org.glassfish.jersey.client.JerseyClientBuilder");
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
        checkJsonTypeInfo(type, JACKSON_2_JSON_TYPE_INFO);
        checkJsonTypeInfo(type, JACKSON_3_JSON_TYPE_INFO);
        checkJsonSubTypes(type, JACKSON_2_JSON_SUB_TYPES);
        checkJsonSubTypes(type, JACKSON_3_JSON_SUB_TYPES);
    }

    private static void checkDeserialize(AnnotatedElement type) {
        checkDeserialize(type, JACKSON_2_JSON_DESERIALIZE);
        checkDeserialize(type, JACKSON_3_JSON_DESERIALIZE);
    }

    @SuppressWarnings("unchecked")
    private static void checkDeserialize(AnnotatedElement type, String annotationName) {
        try {
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) Class.forName(annotationName);
            Annotation deser = type.getAnnotation(annotationType);
            if (deser != null) {
                registerIfNecessary(annotationValue(deser, "builder"));
                registerIfNecessary(annotationValue(deser, "as"));
                registerIfNecessary(annotationValue(deser, "contentAs"));
                registerIfNecessary(annotationValue(deser, "keyAs"));
                registerIfNecessary(annotationValue(deser, "using"));
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkJsonTypeInfo(AnnotatedElement type, String annotationName) {
        try {
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) Class.forName(annotationName);
            Annotation typeInfo = type.getAnnotation(annotationType);
            if (typeInfo != null) {
                Class<?> defaultImpl = annotationValue(typeInfo, "defaultImpl");
                if (defaultImpl != annotationType) {
                    registerIfNecessary(defaultImpl);
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkJsonSubTypes(AnnotatedElement type, String annotationName) {
        try {
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) Class.forName(annotationName);
            Annotation subTypes = type.getAnnotation(annotationType);
            if (subTypes != null) {
                Annotation[] subTypeValues = (Annotation[]) subTypes.annotationType().getMethod("value").invoke(subTypes);
                if (ArrayUtils.isNotEmpty(subTypeValues)) {
                    for (Annotation subType : subTypeValues) {
                        registerIfNecessary(annotationValue(subType, "value"));
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static Class<?> annotationValue(Annotation annotation, String member) throws ReflectiveOperationException {
        return (Class<?>) annotation.annotationType().getMethod(member).invoke(annotation);
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
