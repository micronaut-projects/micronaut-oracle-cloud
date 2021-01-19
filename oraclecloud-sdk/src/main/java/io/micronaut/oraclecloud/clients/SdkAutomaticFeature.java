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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.oracle.bmc.http.internal.ResponseHelper;
import com.oracle.svm.core.annotate.*;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.graal.AutomaticFeatureUtils;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.InstantiationUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.inject.BeanDefinitionReference;
import net.minidev.json.JSONStyle;
import net.minidev.json.reader.BeansWriter;
import net.minidev.json.reader.JsonWriterI;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

@AutomaticFeature
@Internal
final class SdkAutomaticFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {

        // rest client config
        Arrays.asList(
                "org.glassfish.jersey.internal.LocalizationMessages",
                "org.glassfish.jersey.message.internal.MediaTypeProvider",
                "org.glassfish.jersey.message.internal.CacheControlProvider",
                "org.glassfish.jersey.message.internal.LinkProvider").forEach((n) ->
                AutomaticFeatureUtils.initializeAtBuildTime(access, n)
        );
        // setup BC security
        AnnotationMetadata annotationMetadata = getAnnotationMetadata(access);

        if (annotationMetadata != null) {
            Set<Class<?>> reflectiveAccess = new HashSet<>();
            populateReflectionData(reflectiveAccess, ResponseHelper.ErrorCodeAndMessage.class);
            String[] classes = annotationMetadata.stringValues(SdkClients.class);
            for (String aClass : classes) {
                final String packageName = NameUtils.getPackageName(aClass);
                final String simpleName = NameUtils.getSimpleName(aClass);
                final String factoryName = packageName.replace("com.oracle.bmc", "io.micronaut.oraclecloud.clients") + "." + simpleName + "Factory";
                AutomaticFeatureUtils.initializeAtRunTime(access, factoryName);
                Class<?> c = access.findClassByName(aClass);
                if (c != null) {
                    Set<Class> allInterfaces = ReflectionUtils.getAllInterfaces(c);
                    for (Class i : allInterfaces) {
                        if (i.getName().endsWith("Async")) {
                            continue;
                        }
                        populateReflectionData(reflectiveAccess, i);
                    }
                }
            }

            for (Class<?> type : reflectiveAccess) {
                boolean hasNoArgsConstructor = !type.isEnum() &&
                        !type.isInterface() &&
                        hasNoArgsConstructor(type.getDeclaredConstructors());

                RuntimeReflection.register(type);
                if (hasNoArgsConstructor) {
                    RuntimeReflection.registerForReflectiveInstantiation(type);
                }
                for (Method declaredMethod : type.getDeclaredMethods()) {
                    RuntimeReflection.register(declaredMethod);
                }
                if (!type.isInterface()) {
                    for (Field declaredField : type.getDeclaredFields()) {
                        RuntimeReflection.register(declaredField);
                    }
                }
            }
        }
    }

    private boolean hasNoArgsConstructor(Constructor<?>[] declaredConstructors) {
        boolean hasNoArgsConstructor = false;
        for (Constructor<?> declaredConstructor : declaredConstructors) {
            if (declaredConstructor.getParameterCount() == 0) {
                hasNoArgsConstructor = true;
                break;
            }
        }
        return hasNoArgsConstructor;
    }

    static void populateReflectionData(Set<Class<?>> reflectiveAccess, Class<?> type) {
        JsonDeserialize deser = type.getAnnotation(JsonDeserialize.class);
        if (deser != null) {
            Class<?> builder = deser.builder();
            if (builder != Void.class && includeInReflectiveData(reflectiveAccess, builder)) {
                reflectiveAccess.add(builder);
                populateReflectionData(reflectiveAccess, builder);
            }
        }
        Method[] methods = type.getDeclaredMethods();
        for (Method m : methods) {
            Class<?> rt = m.getReturnType();
            if (Collection.class.isAssignableFrom(rt)) {
                Type grt = m.getGenericReturnType();
                if (grt instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) grt).getActualTypeArguments();
                    if (args != null && args.length == 1) {
                        Type arg = args[0];
                        if (arg instanceof Class && includeInReflectiveData(reflectiveAccess, arg)) {
                            Class<?> t = (Class<?>) arg;
                            reflectiveAccess.add(t);
                            populateReflectionData(reflectiveAccess, t);
                        }
                    }
                }
            } else {
                if (includeInReflectiveData(reflectiveAccess, rt)) {
                    reflectiveAccess.add(rt);
                    populateReflectionData(reflectiveAccess, rt);
                }
            }
            Class<?>[] parameterTypes = m.getParameterTypes();
            for (Class<?> pt : parameterTypes) {
                if (includeInReflectiveData(reflectiveAccess, pt)) {
                    reflectiveAccess.add(pt);
                    populateReflectionData(reflectiveAccess, pt);
                }
            }
        }
    }

    static boolean includeInReflectiveData(Set<Class<?>> reflectiveAccess, Type rt) {
        return rt.getTypeName().startsWith("com.oracle.bmc") && !reflectiveAccess.contains(rt);
    }

    private AnnotationMetadata getAnnotationMetadata(BeforeAnalysisAccess access) {
        String targetClass = SdkAutomaticFeatureMetadata.class.getPackage().getName();
        return getAnnotationMetadata(access, targetClass);
    }

    private AnnotationMetadata getAnnotationMetadata(BeforeAnalysisAccess access, String targetClass) {
        return getBeanReference(access, targetClass)
                .map(BeanDefinitionReference::getAnnotationMetadata)
                .orElse(AnnotationMetadata.EMPTY_METADATA);
    }

    private Optional<BeanDefinitionReference<?>> getBeanReference(BeforeAnalysisAccess access, String targetClass) {
        String className = targetClass + ".$" + SdkAutomaticFeatureMetadata.class.getSimpleName() + "DefinitionClass";
        Class<?> featureClass = access.findClassByName(className);
        if (featureClass != null) {
            Object o = InstantiationUtils.instantiate(featureClass);
            if (o instanceof BeanDefinitionReference) {
                return Optional.ofNullable((BeanDefinitionReference<?>) o);
            }
        }
        return Optional.empty();
    }
}
//CHECKSTYLE:OFF
@SuppressWarnings("unused")
@Internal
@TargetClass(className = "net.minidev.json.reader.JsonWriter", onlyWith = JwtNotPresent.class)
final class JsonWriterReplacement {
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    @Alias
    public static JsonWriterI<Object> beansWriterASM = new BeansWriter();
}

@SuppressWarnings("unused")
@Internal
@TargetClass(className = "net.minidev.json.reader.BeansWriterASM", onlyWith = JwtNotPresent.class)
final class BeansWriterASMReplacement {
    @Substitute
    public <E> void writeJSONString(E value, Appendable out, JSONStyle compression) throws IOException {
        new BeansWriter().writeJSONString(value, out, compression);
    }
}

// condition that is checks if JWT is not present
final class JwtNotPresent implements Predicate<String> {
    @Override
    public boolean test(String s) {
        return !ClassUtils.isPresent("io.micronaut.security.token.jwt.config.JwtConfiguration", getClass().getClassLoader());
    }
}
//CHECKSTYLE:ON
