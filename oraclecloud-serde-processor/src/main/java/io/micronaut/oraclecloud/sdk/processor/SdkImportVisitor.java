/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.oraclecloud.sdk.processor;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementQuery;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.serde.annotation.SerdeImport;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.AnnotationDef;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.ParameterDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Visitor that handles the {@code io.micronaut.oraclecloud.core.sdk.SdkImport} annotation.
 *
 * @since 5.3.0
 */
public class SdkImportVisitor implements TypeElementVisitor<Object, Object> {

    public static final String ANN_SDK_IMPORT = "io.micronaut.oraclecloud.core.sdk.SdkImport";
    public static final ClassTypeDef FACTORY_TYPE = ClassTypeDef.of("io.micronaut.oraclecloud.core.sdk.AbstractSdkClientFactory");
    public static final ClassTypeDef TYPE_AUTH_DETAILS_PROVIDER = ClassTypeDef.of("com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider");
    public static final TypeDef TYPE_REGION_PROVIDER = TypeDef.of("com.oracle.bmc.auth.RegionProvider");
    public static final ClassTypeDef TYPE_CLIENT_CONFIGURATION = ClassTypeDef.of("com.oracle.bmc.ClientConfiguration");
    public static final ClassTypeDef TYPE_CLIENT_CONFIGURATOR = ClassTypeDef.of("com.oracle.bmc.http.ClientConfigurator");
    public static final TypeDef TYPE_LIST_OF_CLIENT_CONFIGURATOR = TypeDef.parameterized(
        ClassTypeDef.of("java.util.List"),
        TYPE_CLIENT_CONFIGURATOR
    );
    public static final ClassTypeDef TYPE_REQUEST_SIGNER_FACTORY = ClassTypeDef.of("com.oracle.bmc.http.signing.RequestSignerFactory");
    public static final ClassTypeDef TYPE_REGION = ClassTypeDef.of("com.oracle.bmc.Region");
    public static final ClassTypeDef TYPE_INTERNAL_BUILDER_ACCESS = ClassTypeDef.of("com.oracle.bmc.common.InternalBuilderAccess");
    private ClassElement asyncClientType;
    private ClassElement syncClientType;

    @Override
    public @NonNull VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public TypeElementQuery query() {
        return TypeElementQuery.onlyClass();
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(ANN_SDK_IMPORT);
    }

    @Override
    public void start(VisitorContext visitorContext) {
        this.asyncClientType = visitorContext.getClassElement("com.oracle.bmc.http.internal.BaseAsyncClient").orElse(null);
        this.syncClientType = visitorContext.getClassElement("com.oracle.bmc.http.internal.BaseSyncClient").orElse(null);
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        AnnotationValue<?> importAnn = element.getAnnotation(ANN_SDK_IMPORT);
        if (importAnn != null && asyncClientType != null && syncClientType != null) {
            AnnotationClassValue<?> acv = importAnn.annotationClassValue(AnnotationMetadata.VALUE_MEMBER).orElse(null);
            if (acv != null) {
                context.getClassElement(acv.getName()).ifPresent(classElement -> {
                    if (classElement.isAssignable(asyncClientType) || classElement.isAssignable(syncClientType)) {
                        generateClientFactory(element, classElement, context);
                    }
                });
            }
        }
    }

    private void generateClientFactory(
        ClassElement originatingElement,
        ClassElement clientElement,
        VisitorContext context) {

        ClassElement clientBuilder = context.getClassElement(clientElement.getName() + ".Builder").orElse(null);
        if (clientBuilder == null) {
            context.fail("Unable to import SDK, client has no Builder: " + clientElement.getName(), originatingElement);
        } else {
            ClassTypeDef clientBuilderDef = ClassTypeDef.of(clientBuilder);
            ClassTypeDef clientDef = ClassTypeDef.of(clientElement);

            // generate factory type for client that extends from AbstractSdkClientFactory
            AnnotationDef requiresAnn = AnnotationDef.builder(Requires.class)
                .addMember("classes", clientDef)
                .addMember("beans", TYPE_AUTH_DETAILS_PROVIDER)
                .build();
            String simpleName = clientElement.getSimpleName();
            String serviceId = resolveServiceId(simpleName);
            String typeName = originatingElement.getPackageName() + "." + simpleName + "Factory";
            ClassDef.ClassDefBuilder builder = ClassDef
                .builder(typeName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(Factory.class)
                .addAnnotation(requiresAnn)
                .addAnnotation(AnnotationDef.builder(SerdeImport.class)
                    .addMember("packageName", clientElement.getPackageName() + ".model")
                    .build())
                .superclass(TypeDef.parameterized(FACTORY_TYPE, clientBuilderDef, clientDef));

            // RegionProvider field
            FieldDef regionProviderField = FieldDef.builder("regionProvider", TYPE_REGION_PROVIDER)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
            builder.addField(regionProviderField);

            // generate constructor
            builder.addMethod(MethodDef.constructor()
                .addModifiers(Modifier.PROTECTED)
                    .addParameters(List.of(
                        ParameterDef.builder("clientConfiguration", TYPE_CLIENT_CONFIGURATION).build(),
                        ParameterDef.builder("specificClientConfiguration", TYPE_CLIENT_CONFIGURATION)
                            .addAnnotation(Nullable.class)
                            .addAnnotation(AnnotationDef.builder(Named.class)
                                .addMember(AnnotationMetadata.VALUE_MEMBER, serviceId)
                                .build())
                            .build(),
                        ParameterDef.builder("serviceIdClientConfigurator", TYPE_CLIENT_CONFIGURATOR)
                            .addAnnotation(Nullable.class)
                            .addAnnotation(AnnotationDef.builder(Named.class)
                                .addMember(AnnotationMetadata.VALUE_MEMBER, serviceId)
                                .build())
                            .build(),
                        ParameterDef.builder("clientConfiguratorList", TYPE_LIST_OF_CLIENT_CONFIGURATOR)
                            .addAnnotation(Nullable.class)
                            .build(),
                        ParameterDef.builder("requestSignerFactory", TYPE_REQUEST_SIGNER_FACTORY)
                            .addAnnotation(Nullable.class)
                            .build(),
                        ParameterDef.builder("regionProvider", TYPE_REGION_PROVIDER)
                            .addAnnotation(Nullable.class)
                            .build())
                    )
                .build((aThis, methodParameters) -> {
                        VariableDef.MethodParameter regionProvider = methodParameters.get(5);
                        return StatementDef.multi(
                            aThis.superRef().invokeConstructor(
                                clientDef.invokeStatic("builder", clientBuilderDef),
                                methodParameters.get(0),
                                methodParameters.get(1),
                                methodParameters.get(2),
                                methodParameters.get(3),
                                methodParameters.get(4)),
                            aThis.field(regionProviderField).assign(regionProvider)
                        );
                    }
                ));


            // generate builder method
            builder.addMethod(
                MethodDef.builder("getBuilder")
                    .returns(clientBuilderDef)
                    .addModifiers(Modifier.PROTECTED)
                    .addAnnotation(Singleton.class)
                    .addAnnotation(requiresAnn)
                    .build((aThis, params) ->
                        aThis.superRef().invoke("getBuilder", clientBuilderDef).returning()
                    )
            );

            // generate build method
            builder.addMethod(
                MethodDef.builder("build")
                    .returns(clientDef)
                    .addParameters(
                        clientBuilderDef,
                        TYPE_AUTH_DETAILS_PROVIDER
                    )
                    .addModifiers(Modifier.PROTECTED)
                    .addAnnotation(Singleton.class)
                    .addAnnotation(requiresAnn)
                    .addAnnotation(AnnotationDef.builder(Bean.class)
                        .addMember("preDestroy", "close")
                        .build())
                    .build((aThis, params) -> {
                        VariableDef.MethodParameter clientBuilderParam = params.get(0);
                        VariableDef.MethodParameter authDetails = params.get(1);
                        VariableDef.Field field = aThis.field(regionProviderField);
                        ExpressionDef.ConditionExpressionDef condition = field.isNonNull().and(
                            field.invoke("getRegion", TYPE_REGION).isNonNull()
                        ).and(
                            TYPE_INTERNAL_BUILDER_ACCESS
                                .invokeStatic("getEndpoint", ClassTypeDef.STRING, clientBuilderParam)
                                .isNull()
                        );


                        return condition.ifTrue(
                            clientBuilderParam.invoke("region", clientBuilderDef, field.invoke("getRegion", TYPE_REGION))
                                .invoke("build", clientDef, authDetails).returning(),
                            clientBuilderParam.invoke("build", clientDef, authDetails).returning()
                        );
                        }
                    )
            );

            // write class
            ClassDef factoryDef = builder.build();
            SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
            if (sourceGenerator != null) {
                sourceGenerator.write(factoryDef, context, originatingElement);
            } else {
                context.fail("Cannot process @SdkImport(). Missing SourceGenerator module from annotation processor path (for example micronaut-sourcegen-generator-java for Java).", originatingElement);
            }
        }
    }

    private static String resolveServiceId(String simpleName) {
        String serviceId;
        if (simpleName.endsWith("AsyncClient")) {
            serviceId = simpleName.substring(0, simpleName.length() - "AsyncClient".length()).toLowerCase(Locale.ENGLISH);
        } else if (simpleName.endsWith("Client")) {
            serviceId = simpleName.substring(0, simpleName.length() - "Client".length()).toLowerCase(Locale.ENGLISH);
        } else {
            serviceId = simpleName.toLowerCase(Locale.ENGLISH);
        }
        return serviceId;
    }
}
