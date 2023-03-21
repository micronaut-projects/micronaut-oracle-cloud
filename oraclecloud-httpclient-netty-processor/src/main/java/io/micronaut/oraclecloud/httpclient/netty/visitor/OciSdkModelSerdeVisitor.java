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
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ConstructorElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.TypedElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.Optional;

/**
 * Type element visitor vising oci sdk models and enums.
 *
 * @author Andriy Dmytruk
 * @since 4.0.0
 */
@Internal
public class OciSdkModelSerdeVisitor implements TypeElementVisitor<Object, Object> {

    private static final String ANN_SERDEABLE = "io.micronaut.serde.annotation.Serdeable";
    private static final String ANN_SERDE_CONFIG = "io.micronaut.serde.config.annotation.SerdeConfig";
    private static final String OCI_SDK_MODEL_CLASS_NAME = "com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel";
    private static final String OCI_SDK_ENUM_CLASS_NAME = "com.oracle.bmc.http.internal.BmcEnum";
    private static final String OCI_SDK_ENUM_CREATOR_NAME = "create";

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
            element.annotate(ANN_SERDEABLE);
            ignoreMicronautSerdeValidation(element);
        } else if (visitingOciSdkEnum) {
            element.annotate(ANN_SERDEABLE);
        }
    }

    private void ignoreMicronautSerdeValidation(ClassElement element) {
        element.annotate(
            AnnotationValue.builder(ANN_SERDE_CONFIG)
                .member("validate", false)
                .build()
        );
    }

    @Override
    public void visitField(FieldElement element, VisitorContext context) {
        if (visitingOciSdkModel) {
            visitElement(element);
        }
    }

    @Override
    public void visitConstructor(ConstructorElement element, VisitorContext context) {
        if (visitingOciSdkModel) {
            for (ParameterElement parameter: element.getParameters()) {
                visitElement(parameter);
            }
        }
    }

    @Override
    public void visitMethod(MethodElement element, VisitorContext context) {
        if (visitingOciSdkEnum) {
            if (element.getName().equals(OCI_SDK_ENUM_CREATOR_NAME)) {
                for (ParameterElement parameter: element.getParameters()) {
                    visitElement(parameter);
                }
            }
        }
    }

    private void visitElement(TypedElement element) {
        if (!element.isNonNull() && !element.isDeclaredNullable()) {
            element.annotate(AnnotationUtil.NULLABLE);
        }
    }

    private static boolean isOciSdkModel(ClassElement element) {
        Optional<ClassElement> parent = element.getSuperType();
        boolean isOciSdkModel = false;

        while (parent.isPresent()) {
            if (parent.get().getName().equals(OCI_SDK_MODEL_CLASS_NAME)) {
                isOciSdkModel = true;
                break;
            }
            parent = parent.get().getSuperType();
        }

        if (element.getName().equals("com.oracle.bmc.http.internal.ResponseHelper$ErrorCodeAndMessage")) {
            return true;
        }

        return isOciSdkModel;
    }

    private static boolean isOciSdkEnum(ClassElement element) {
        return element.getInterfaces().stream()
            .anyMatch(i -> i.getName().equals(OCI_SDK_ENUM_CLASS_NAME));
    }
}
