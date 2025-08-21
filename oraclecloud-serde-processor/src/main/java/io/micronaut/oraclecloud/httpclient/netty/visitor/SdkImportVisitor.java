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
package io.micronaut.oraclecloud.httpclient.netty.visitor;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.common.InternalBuilderAccess;
import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.internal.BaseAsyncClient;
import com.oracle.bmc.http.internal.BaseSyncClient;
import com.oracle.bmc.http.signing.RequestSignerFactory;
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
import io.micronaut.oraclecloud.core.sdk.AbstractSdkClientFactory;
import io.micronaut.oraclecloud.core.sdk.SdkImport;
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
 * Visitor that handles the {@link SdkImport} annotation.
 */
public class SdkImportVisitor implements TypeElementVisitor<SdkImport, Object> {
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
        return Set.of(SdkImport.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        AnnotationValue<SdkImport> importAnn = element.getAnnotation(SdkImport.class);
        if (importAnn != null) {
            AnnotationClassValue<?> acv = importAnn.annotationClassValue(AnnotationMetadata.VALUE_MEMBER).orElse(null);
            if (acv != null) {
                context.getClassElement(acv.getName()).ifPresent(classElement -> {
                    if (classElement.isAssignable(BaseAsyncClient.class) || classElement.isAssignable(BaseSyncClient.class)) {
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
        if (clientBuilder != null) {
            ClassTypeDef clientBuilderDef = ClassTypeDef.of(clientBuilder);
            ClassTypeDef clientDef = ClassTypeDef.of(clientElement);

            // generate factory type for client that extends from AbstractSdkClientFactory
            AnnotationDef requiresAnn = AnnotationDef.builder(Requires.class)
                .addMember("classes", clientDef)
                .addMember("beans", ClassTypeDef.of(AbstractAuthenticationDetailsProvider.class))
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
                .superclass(TypeDef.parameterized(ClassTypeDef.of(AbstractSdkClientFactory.class), clientBuilderDef, clientDef));

            // RegionProvider field
            FieldDef regionProviderField = FieldDef.builder("regionProvider", TypeDef.of(RegionProvider.class))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
            builder.addField(regionProviderField);

            // generate constructor
            builder.addMethod(MethodDef.constructor()
                .addModifiers(Modifier.PROTECTED)
                    .addParameters(List.of(
                        ParameterDef.builder("clientConfiguration", TypeDef.of(ClientConfiguration.class)).build(),
                        ParameterDef.builder("specificClientConfiguration", TypeDef.of(ClientConfiguration.class))
                            .addAnnotation(Nullable.class)
                            .addAnnotation(AnnotationDef.builder(Named.class)
                                .addMember(AnnotationMetadata.VALUE_MEMBER, serviceId)
                                .build())
                            .build(),
                        ParameterDef.builder("serviceIdClientConfigurator", TypeDef.of(ClientConfigurator.class))
                            .addAnnotation(Nullable.class)
                            .addAnnotation(AnnotationDef.builder(Named.class)
                                .addMember(AnnotationMetadata.VALUE_MEMBER, serviceId)
                                .build())
                            .build(),
                        ParameterDef.builder("clientConfigurator", TypeDef.of(ClientConfigurator.class))
                            .addAnnotation(Nullable.class)
                            .build(),
                        ParameterDef.builder("requestSignerFactory", TypeDef.of(RequestSignerFactory.class))
                            .addAnnotation(Nullable.class)
                            .build(),
                        ParameterDef.builder("regionProvider", TypeDef.of(RegionProvider.class))
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
                        TypeDef.of(AbstractAuthenticationDetailsProvider.class)
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
                            field.invoke("getRegion", TypeDef.of(Region.class)).isNonNull()
                        ).and(
                            ClassTypeDef.of(InternalBuilderAccess.class)
                                .invokeStatic("getEndpoint", ClassTypeDef.STRING, clientBuilderParam)
                                .isNull()
                        );


                        return condition.ifTrue(
                            clientBuilderParam.invoke("region", clientBuilderDef, field.invoke("getRegion", TypeDef.of(Region.class)))
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
