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
package io.micronaut.oraclecloud.httpclient.netty.visitor;

import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ConstructorElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.serde.config.annotation.SerdeConfig;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Type element visitor vising oci sdk models and enums.
 * <br/>
 * Initiates the creation of serializable introspections for the models and enums. The introspections
 * are written to a separate .introspection package. The introspections need to be used with the
 * oraclecloud-httpclient-netty which defines the explicitly set property filter required for
 * correct serialization.
 *
 * @author Andriy Dmytruk
 * @since 2.3.2
 */
@Internal
public class OciSdkModelSerdeVisitor implements TypeElementVisitor<Object, Object> {
    private static final String OCI_SDK_MODEL_CLASS_NAME = "com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel";
    private static final String OCI_SDK_ENUM_CLASS_NAME = "com.oracle.bmc.http.internal.BmcEnum";
    private static final String OCI_SDK_ENUM_CREATOR_NAME = "create";
    private static final String INTROSPECTION_PACKAGE = ".introspection";

    private static final Set<String> ADDITIONAL_MODELS = Set.of(
        "com.oracle.bmc.http.internal.ResponseHelper$ErrorCodeAndMessage",
        "com.oracle.bmc.model.RegionSchema",
        "com.oracle.bmc.auth.internal.X509FederationClient$SecurityToken"
    );

    private boolean visitingOciSdkModel;
    private boolean visitingOciSdkEnum;

    @Override
    public int getOrder() {
        return 10; // Should run before SerdeAnnotationVisitor
    }

    @NonNull
    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        visitingOciSdkModel = isOciSdkModel(element);
        visitingOciSdkEnum = isOciSdkEnum(element);

        if (visitingOciSdkModel) {
            makeSerdeable(element);
            // Ignore the validation because the Deserialize(builder=) is not supported
            ignoreMicronautSerdeValidation(element);
        } else if (visitingOciSdkEnum) {
            makeSerdeable(element);
            element.getMethods().stream()
                .filter(m -> m.getName().equals(OCI_SDK_ENUM_CREATOR_NAME))
                .findAny()
                .ifPresent(OciSdkModelSerdeVisitor::makeParametersNullable);
        }
    }

    @Override
    public void visitField(FieldElement element, VisitorContext context) {
        if (visitingOciSdkModel) {
            makeElementNullable(element);
        }
    }

    @Override
    public void visitConstructor(ConstructorElement element, VisitorContext context) {
        if (visitingOciSdkModel) {
            makeParametersNullable(element);
        }
    }

    @Override
    public void visitMethod(MethodElement element, VisitorContext context) {
        if (visitingOciSdkEnum && element.getName().equals(OCI_SDK_ENUM_CREATOR_NAME)) {
            makeParametersNullable(element);
        }
    }

    private static void makeParametersNullable(MethodElement element) {
        Arrays.stream(element.getParameters()).forEach(OciSdkModelSerdeVisitor::makeElementNullable);
    }

    private static void makeElementNullable(TypedElement element) {
        if (!element.isNonNull() && !element.isDeclaredNullable()) {
            element.annotate(AnnotationUtil.NULLABLE);
        }
    }

    private void makeSerdeable(ClassElement element) {
        // Add Serdeable annotation and all its stereotypes
        element.annotate(Serdeable.class);
        element.annotate(Serdeable.Serializable.class);
        element.annotate(Serdeable.Deserializable.class);
        // Write the introspections to a different package, to avoid different signers for classes
        // in the save package, since the original classes are added as a dependency
        element.annotate(Introspected.class,
            builder -> builder.member("targetPackage", element.getPackageName() + INTROSPECTION_PACKAGE)
        );
    }

    private void ignoreMicronautSerdeValidation(Element element) {
        element.annotate(
            AnnotationValue.builder(SerdeConfig.class)
                .member("validate", false)
                .build()
        );
    }

    private static boolean isOciSdkModel(ClassElement element) {
        Optional<ClassElement> parent = element.getSuperType();
        while (parent.isPresent()) {
            if (parent.get().getName().equals(OCI_SDK_MODEL_CLASS_NAME)) {
                return true;
            }
            parent = parent.get().getSuperType();
        }

        return ADDITIONAL_MODELS.contains(element.getName());
    }

    private static boolean isOciSdkEnum(ClassElement element) {
        return element.getInterfaces().stream()
            .anyMatch(i -> i.getName().equals(OCI_SDK_ENUM_CLASS_NAME));
    }
}
