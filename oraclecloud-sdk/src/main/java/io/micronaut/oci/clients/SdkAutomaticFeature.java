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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
import com.oracle.bmc.cims.IncidentAsyncClient;
import com.oracle.bmc.cims.IncidentClient;
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
import com.oracle.bmc.dts.*;
import com.oracle.bmc.email.EmailAsyncClient;
import com.oracle.bmc.email.EmailClient;
import com.oracle.bmc.events.EventsAsyncClient;
import com.oracle.bmc.events.EventsClient;
import com.oracle.bmc.filestorage.FileStorageAsyncClient;
import com.oracle.bmc.filestorage.FileStorageClient;
import com.oracle.bmc.functions.FunctionsInvokeAsyncClient;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.FunctionsManagementAsyncClient;
import com.oracle.bmc.functions.FunctionsManagementClient;
import com.oracle.bmc.healthchecks.HealthChecksAsyncClient;
import com.oracle.bmc.healthchecks.HealthChecksClient;
import com.oracle.bmc.http.internal.ResponseHelper;
import com.oracle.bmc.identity.IdentityAsyncClient;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.integration.IntegrationInstanceAsyncClient;
import com.oracle.bmc.integration.IntegrationInstanceClient;
import com.oracle.bmc.keymanagement.KmsVaultAsyncClient;
import com.oracle.bmc.keymanagement.KmsVaultClient;
import com.oracle.bmc.limits.*;
import com.oracle.bmc.loadbalancer.LoadBalancerAsyncClient;
import com.oracle.bmc.loadbalancer.LoadBalancerClient;
import com.oracle.bmc.marketplace.MarketplaceAsyncClient;
import com.oracle.bmc.marketplace.MarketplaceClient;
import com.oracle.bmc.monitoring.MonitoringAsyncClient;
import com.oracle.bmc.monitoring.MonitoringClient;
import com.oracle.bmc.mysql.*;
import com.oracle.bmc.nosql.NosqlAsyncClient;
import com.oracle.bmc.nosql.NosqlClient;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.oce.OceInstanceAsyncClient;
import com.oracle.bmc.oce.OceInstanceClient;
import com.oracle.bmc.ocvp.*;
import com.oracle.bmc.oda.OdaAsyncClient;
import com.oracle.bmc.oda.OdaClient;
import com.oracle.bmc.ons.NotificationControlPlaneAsyncClient;
import com.oracle.bmc.ons.NotificationControlPlaneClient;
import com.oracle.bmc.ons.NotificationDataPlaneAsyncClient;
import com.oracle.bmc.ons.NotificationDataPlaneClient;
import com.oracle.bmc.osmanagement.OsManagementAsyncClient;
import com.oracle.bmc.osmanagement.OsManagementClient;
import com.oracle.bmc.resourcemanager.ResourceManagerAsyncClient;
import com.oracle.bmc.resourcemanager.ResourceManagerClient;
import com.oracle.bmc.resourcesearch.ResourceSearchAsyncClient;
import com.oracle.bmc.resourcesearch.ResourceSearchClient;
import com.oracle.bmc.secrets.SecretsAsyncClient;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.streaming.StreamAdminAsyncClient;
import com.oracle.bmc.streaming.StreamAdminClient;
import com.oracle.bmc.usageapi.UsageapiAsyncClient;
import com.oracle.bmc.usageapi.UsageapiClient;
import com.oracle.bmc.vault.VaultsAsyncClient;
import com.oracle.bmc.vault.VaultsClient;
import com.oracle.bmc.waas.RedirectAsyncClient;
import com.oracle.bmc.waas.RedirectClient;
import com.oracle.bmc.waas.WaasAsyncClient;
import com.oracle.bmc.waas.WaasClient;
import com.oracle.svm.core.annotate.*;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.graal.AutomaticFeatureUtils;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.InstantiationUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinitionReference;
import net.minidev.json.JSONStyle;
import net.minidev.json.reader.BeansWriter;
import net.minidev.json.reader.JsonWriterI;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;


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
        DbBackupsClient.class,
        DbBackupsAsyncClient.class,
        DbSystemClient.class,
        DbSystemAsyncClient.class,
        EmailClient.class,
        EmailAsyncClient.class,
        EsxiHostClient.class,
        EsxiHostAsyncClient.class,
        EventsClient.class,
        EventsAsyncClient.class,
        FileStorageClient.class,
        FileStorageAsyncClient.class,
        FunctionsInvokeClient.class,
        FunctionsInvokeAsyncClient.class,
        FunctionsManagementClient.class,
        FunctionsManagementAsyncClient.class,
        GatewayClient.class,
        GatewayAsyncClient.class,
        HealthChecksClient.class,
        HealthChecksAsyncClient.class,
        IdentityClient.class,
        IdentityAsyncClient.class,
        IncidentClient.class,
        IncidentAsyncClient.class,
        IntegrationInstanceClient.class,
        IntegrationInstanceAsyncClient.class,
        KmsVaultAsyncClient.class,
        KmsVaultClient.class,
        LimitsClient.class,
        LimitsAsyncClient.class,
        LoadBalancerClient.class,
        LoadBalancerAsyncClient.class,
        MarketplaceClient.class,
        MarketplaceAsyncClient.class,
        MonitoringClient.class,
        MonitoringAsyncClient.class,
        MysqlaasClient.class,
        MysqlaasAsyncClient.class,
        NosqlClient.class,
        NosqlAsyncClient.class,
        NotificationControlPlaneClient.class,
        NotificationControlPlaneAsyncClient.class,
        NotificationDataPlaneClient.class,
        NotificationDataPlaneAsyncClient.class,
        ObjectStorageClient.class,
        ObjectStorageAsyncClient.class,
        OceInstanceClient.class,
        OceInstanceAsyncClient.class,
        OdaClient.class,
        OdaAsyncClient.class,
        OsManagementClient.class,
        OsManagementAsyncClient.class,
        QuotasClient.class,
        QuotasAsyncClient.class,
        RedirectClient.class,
        RedirectAsyncClient.class,
        ResourceManagerClient.class,
        ResourceManagerAsyncClient.class,
        ResourceSearchClient.class,
        ResourceSearchAsyncClient.class,
        SddcClient.class,
        SddcAsyncClient.class,
        SecretsClient.class,
        SecretsAsyncClient.class,
        ShippingVendorsClient.class,
        ShippingVendorsAsyncClient.class,
        StreamAdminClient.class,
        StreamAdminAsyncClient.class,
        TransferApplianceClient.class,
        TransferApplianceAsyncClient.class,
        TransferApplianceEntitlementClient.class,
        TransferApplianceEntitlementAsyncClient.class,
        TransferDeviceClient.class,
        TransferDeviceAsyncClient.class,
        TransferJobClient.class,
        TransferJobAsyncClient.class,
        TransferPackageClient.class,
        TransferPackageAsyncClient.class,
        UsageapiClient.class,
        UsageapiAsyncClient.class,
        VaultsClient.class,
        VaultsAsyncClient.class,
        VirtualNetworkClient.class,
        VirtualNetworkAsyncClient.class,
        WaasClient.class,
        WaasAsyncClient.class,
        WorkRequestClient.class,
        WorkRequestAsyncClient.class,
        com.oracle.bmc.workrequests.WorkRequestClient.class,
        com.oracle.bmc.workrequests.WorkRequestAsyncClient.class,
        WorkRequestsClient.class,
        WorkRequestsAsyncClient.class,
        com.oracle.bmc.apigateway.WorkRequestsClient.class,
        com.oracle.bmc.apigateway.WorkRequestsAsyncClient.class

})
@Requires(property = "oci.sdk.compiler", value = StringUtils.TRUE)
@Singleton
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
                final String factoryName = packageName.replace("com.oracle.bmc", "io.micronaut.oci.clients") + "." + simpleName + "Factory";
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