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
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.graal.AutomaticFeatureUtils;
import io.micronaut.core.io.service.ServiceDefinition;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import net.minidev.json.JSONStyle;
import net.minidev.json.reader.BeansWriter;
import net.minidev.json.reader.JsonWriterI;
import org.glassfish.hk2.utilities.DescriptorImpl;
import org.glassfish.jersey.internal.ServiceConfigurationError;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.jvnet.hk2.internal.SystemDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

@AutomaticFeature
@Internal
@SdkClients(SdkClients.Kind.ASYNC)
final class SdkAutomaticFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {

        // rest client config
        Arrays.asList(
                "org.glassfish.jersey.internal.LocalizationMessages",
                "org.glassfish.jersey.message.internal.MediaTypeProvider",
                "org.glassfish.jersey.message.internal.CacheControlProvider",
                "org.glassfish.jersey.message.internal.LinkProvider").forEach((n) ->
                                                                                      AutomaticFeatureUtils
                                                                                              .initializeAtBuildTime(access, n)
        );
        // setup BC security
        Set<Class<?>> reflectiveAccess = new HashSet<>();
        populateReflectionData(reflectiveAccess, ResponseHelper.ErrorCodeAndMessage.class);
        String[] classes = resolveOracleCloudClientNamesFromManifest().toArray(new String[0]);
        for (String aClass : classes) {
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

    public static List<String> resolveOracleCloudClientNamesFromManifest() {
        try {
            List<String> results = new ArrayList<>();
            final Enumeration<URL> manifests = SdkAutomaticFeature.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (manifests.hasMoreElements()) {
                final URL url = manifests.nextElement();
                if (url.getPath().contains("oci-java")) {
                    try (InputStream is = url.openStream()) {
                        final Manifest manifest = new Manifest(is);
                        final Map<String, Attributes> entries = manifest.getEntries();
                        entries.keySet().stream()
                                .filter((key) -> key.endsWith("Client.class") && !isSdkInternal(key))
                                .forEach((fileName) -> results.add(
                                        fileName.replace('/', '.')
                                                .substring(0, fileName.length() - 6)
                                ));
                    }
                }
            }
            return results;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private static boolean isSdkInternal(String key) {
        return Stream.of(
                "/internal/",
                "/auth/",
                "/streaming/",
                "/keymanagement/"
        ).anyMatch(key::contains);
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

// disable javassist proxies
@TargetClass(org.jvnet.hk2.internal.Utilities.class)
final class HK2UtilsReplacements {
    @Substitute
    public synchronized static boolean proxiesAvailable() {
        return false;
    }
}

@TargetClass(SystemDescriptor.class)
final class SystemDescriptorReplacements {
    @Substitute
    public Boolean isProxiable() {
        return false;
    }
}

@TargetClass(DescriptorImpl.class)
final class DescriptorImplReplacements {
    @Substitute
    public Boolean isProxiable() {
        return false;
    }
}

// replace ServiceFinder to not use custom classloader and javassist
@TargetClass(className = "org.glassfish.jersey.internal.ServiceFinder")
final class ServiceFinderReplacement<T> implements Iterable<T> {

    @Alias
    private Class<T> serviceClass;
    @Alias
    private String serviceName;
    @Alias
    private ClassLoader classLoader;
    @Alias
    private boolean ignoreOnClassNotFound;

    @Substitute
    private ServiceFinderReplacement(
            final Class<T> service,
            final ClassLoader loader,
            final boolean ignoreOnClassNotFound) {
        this(service, service.getName(), loader, ignoreOnClassNotFound);
    }

    @Substitute
    private ServiceFinderReplacement(
            final Class<T> service,
            final String serviceName,
            final ClassLoader loader,
            final boolean ignoreOnClassNotFound) {
        this.serviceClass = service;
        this.serviceName = serviceName;
        this.classLoader = loader;
        this.ignoreOnClassNotFound = ignoreOnClassNotFound;
    }

    @Override
    @Substitute
    public Iterator<T> iterator() {
        return ServiceLoader.load(serviceClass).iterator();
    }

    @Substitute
    public Class<T>[] toClassArray() throws ServiceConfigurationError {
        SoftServiceLoader<T> loader = SoftServiceLoader.load(serviceClass, classLoader);
        List<Class<T>> classes = new ArrayList<>();
        for (ServiceDefinition<T> definition : loader) {
            Class aClass = ClassUtils.forName(definition.getName(), classLoader).orElse(null);
            classes.add(aClass);
        }
        //noinspection unchecked
        return classes.toArray(new Class[0]);
    }
}
//CHECKSTYLE:ON
