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
package io.micronaut.oci.clients;

import com.oracle.bmc.analytics.AnalyticsAsyncClient;
import com.oracle.bmc.analytics.AnalyticsClient;
import com.oracle.bmc.announcementsservice.AnnouncementAsyncClient;
import com.oracle.bmc.announcementsservice.AnnouncementClient;
import com.oracle.bmc.apigateway.GatewayAsyncClient;
import com.oracle.bmc.apigateway.GatewayClient;
import com.oracle.bmc.applicationmigration.ApplicationMigrationAsyncClient;
import com.oracle.bmc.applicationmigration.ApplicationMigrationClient;
import com.oracle.bmc.audit.AuditAsyncClient;
import com.oracle.bmc.audit.AuditClient;
import com.oracle.bmc.autoscaling.AutoScalingAsyncClient;
import com.oracle.bmc.autoscaling.AutoScalingClient;
import com.oracle.bmc.bds.BdsAsyncClient;
import com.oracle.bmc.bds.BdsClient;
import com.oracle.bmc.budget.BudgetAsyncClient;
import com.oracle.bmc.budget.BudgetClient;
import com.oracle.bmc.containerengine.ContainerEngineAsyncClient;
import com.oracle.bmc.containerengine.ContainerEngineClient;
import com.oracle.bmc.core.*;
import com.oracle.bmc.database.DatabaseAsyncClient;
import com.oracle.bmc.database.DatabaseClient;
import com.oracle.bmc.datacatalog.DataCatalogAsyncClient;
import com.oracle.bmc.datacatalog.DataCatalogClient;
import com.oracle.bmc.dataflow.DataFlowAsyncClient;
import com.oracle.bmc.dataflow.DataFlowClient;
import com.oracle.bmc.dataintegration.DataIntegrationAsyncClient;
import com.oracle.bmc.dataintegration.DataIntegrationClient;
import com.oracle.bmc.datasafe.DataSafeClient;
import com.oracle.bmc.datascience.DataScienceAsyncClient;
import com.oracle.bmc.datascience.DataScienceClient;
import com.oracle.bmc.dts.ApplianceExportJobAsyncClient;
import com.oracle.bmc.dts.ApplianceExportJobClient;
import com.oracle.bmc.email.EmailAsyncClient;
import com.oracle.bmc.email.EmailClient;
import com.oracle.bmc.filestorage.FileStorageAsyncClient;
import com.oracle.bmc.filestorage.FileStorageClient;
import com.oracle.bmc.functions.FunctionsInvokeAsyncClient;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.svm.core.annotate.AutomaticFeature;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.graal.AutomaticFeatureUtils;
import io.micronaut.core.reflect.InstantiationUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinitionReference;
import org.graalvm.nativeimage.hosted.Feature;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.*;

@Internal
@AutomaticFeature
@SdkClients({
        AnalyticsClient.class,
        AnalyticsAsyncClient.class,
        AnnouncementClient.class,
        AnnouncementAsyncClient.class,
        ApplianceExportJobClient.class,
        ApplianceExportJobAsyncClient.class,
        ApplicationMigrationClient.class,
        ApplicationMigrationAsyncClient.class,
        AuditClient.class,
        AuditAsyncClient.class,
        AutoScalingClient.class,
        AutoScalingAsyncClient.class,
        BdsClient.class,
        BdsAsyncClient.class,
        BlockstorageClient.class,
        BlockstorageAsyncClient.class,
        BudgetClient.class,
        BudgetAsyncClient.class,
        ComputeClient.class,
        ComputeAsyncClient.class,
        ComputeManagementClient.class,
        ComputeManagementAsyncClient.class,
        ContainerEngineClient.class,
        ContainerEngineAsyncClient.class,
        DataCatalogClient.class,
        DataCatalogAsyncClient.class,
        DataFlowClient.class,
        DataFlowAsyncClient.class,
        DataIntegrationClient.class,
        DataIntegrationAsyncClient.class,
        DataSafeClient.class,
        DataScienceAsyncClient.class,
        DataScienceClient.class,
        DatabaseClient.class,
        DatabaseAsyncClient.class,
        EmailClient.class,
        EmailAsyncClient.class,
        ObjectStorageClient.class,
        ObjectStorageAsyncClient.class,
        FileStorageClient.class,
        FileStorageAsyncClient.class,
        GatewayClient.class,
        GatewayAsyncClient.class,
        FunctionsInvokeClient.class,
        FunctionsInvokeAsyncClient.class
})
@Requires(property = "oci.sdk.compiler", value = StringUtils.TRUE)
@Singleton
final class SdkAutomaticFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {

        // setup BC security
        AutomaticFeatureUtils.addResourcePatterns("javax/crypto/Cipher.class", "org/bouncycastle/MARKER");
        AutomaticFeatureUtils.initializeAtBuildTime(access, "com.oracle.bmc.http.signing.internal.PEMFileRSAPrivateKeySupplier");

        AnnotationMetadata annotationMetadata = getAnnotationMetadata(access);

        if (annotationMetadata != null) {
            List<AnnotationValue<Requires>> values =
                    annotationMetadata.getAnnotationValuesByType(Requires.class);
            Set<Class<?>> reflectiveAccess = new HashSet<>();
            for (AnnotationValue<Requires> value : values) {
                String[] classes = value.stringValues("classes");
                for (String aClass : classes) {
                    Class<?> c = access.findClassByName(aClass);
                    if (c != null) {
                        Set<Class> allInterfaces = ReflectionUtils.getAllInterfaces(c);
                        for (Class i : allInterfaces) {
                            if (i.getName().equals("Async")) {
                                continue;
                            }
                            populateReflectionData(reflectiveAccess, i);
                        }
                    }
                }
            }

            for (Class<?> aClass : reflectiveAccess) {
                AutomaticFeatureUtils.registerAllForRuntimeReflection(access, aClass.getName());
            }
        }
    }

    private void populateReflectionData(Set<Class<?>> reflectiveAccess, Class<?> type) {
        Method[] methods = type.getDeclaredMethods();
        for (Method m : methods) {
            Class<?> rt = m.getReturnType();
            if (includeInReflectiveData(reflectiveAccess, rt)) {
                reflectiveAccess.add(rt);
                populateReflectionData(reflectiveAccess, rt);
            }
            Class<?>[] parameterTypes = m.getParameterTypes();
            for (Class<?> pt : parameterTypes) {
                if (includeInReflectiveData(reflectiveAccess, rt)) {
                    reflectiveAccess.add(pt);
                    populateReflectionData(reflectiveAccess, pt);
                }
            }
        }
    }

    private boolean includeInReflectiveData(Set<Class<?>> reflectiveAccess, Class<?> rt) {
        return rt.getName().startsWith("com.oracle.bmc") && !rt.getName().endsWith("$Builder") && !reflectiveAccess.contains(rt);
    }

    private AnnotationMetadata getAnnotationMetadata(BeforeAnalysisAccess access) {
        String targetClass = SdkAutomaticFeature.class.getPackage().getName();
        return getAnnotationMetadata(access, targetClass);
    }

    private AnnotationMetadata getAnnotationMetadata(BeforeAnalysisAccess access, String targetClass) {
        return getBeanReference(access, targetClass)
                    .map(BeanDefinitionReference::getAnnotationMetadata)
                    .orElse(AnnotationMetadata.EMPTY_METADATA);
    }

    private Optional<BeanDefinitionReference<?>> getBeanReference(BeforeAnalysisAccess access, String targetClass) {
        String className = targetClass + ".$" + SdkAutomaticFeature.class.getSimpleName() + "DefinitionClass";
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
