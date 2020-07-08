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
package io.micronaut.oci.clients.processor;

import com.squareup.javapoet.*;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.naming.NameUtils;

import javax.annotation.processing.*;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An annotation processor that generates the Oracle Cloud SDK integration
 * for Micronaut.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@SupportedAnnotationTypes("io.micronaut.context.annotation.Requires")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class OracleCloudSdkProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            final Set<? extends Element> element = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element e : element) {
                if (e instanceof PackageElement && ((PackageElement) e).getQualifiedName().toString().equals("io.micronaut.oci.clients")) {

                    List<String> clientNames = resolveClientNames(e);

                    for (String clientName : clientNames) {
                        final String packageName = NameUtils.getPackageName(clientName);
                        final String simpleName = NameUtils.getSimpleName(clientName);
                        final String factoryName = simpleName + "Factory";
                        final String factoryPackageName = packageName.replace("com.oracle.bmc", "io.micronaut.oci.clients");
                        final TypeSpec.Builder builder = defineSuperclass(packageName, simpleName, factoryName);
                        final MethodSpec.Builder constructor = buildConstructor(simpleName, builder);
                        final ClassName builderType = ClassName.get(packageName, simpleName + ".Builder");
                        builder.addField(FieldSpec.builder(builderType, "builder", Modifier.PRIVATE).build());
                        builder.addAnnotation(Factory.class);
                        final AnnotationSpec.Builder requiresSpec = AnnotationSpec.builder(Requires.class).addMember("classes", simpleName + ".class");
                        builder.addAnnotation(requiresSpec.build());
                        builder.addMethod(constructor.build());

                        final MethodSpec.Builder getBuilder = MethodSpec.methodBuilder("getBuilder");
                        getBuilder.returns(builderType)
                                .addAnnotation(Singleton.class)
                                .addAnnotation(requiresSpec.build())
                                .addModifiers(Modifier.PROTECTED)
                                .addCode("return super.getBuilder();");
                        builder.addMethod(getBuilder.build());

                        final MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build");
                        buildMethod.returns(ClassName.get(packageName, simpleName))
                                .addParameter(ClassName.get("com.oracle.bmc.auth", "AbstractAuthenticationDetailsProvider"), "authenticationDetailsProvider")
                                .addAnnotation(Singleton.class)
                                .addAnnotation(requiresSpec.build())
                                .addModifiers(Modifier.PROTECTED)
                                .addCode("return builder.build(authenticationDetailsProvider);");
                        builder.addMethod(buildMethod.build());


                        final JavaFile javaFile = JavaFile.builder(factoryPackageName, builder.build()).build();
                        try {
                            final JavaFileObject javaFileObject = filer.createSourceFile(factoryPackageName + "." + factoryName, e);
                            try (final Writer writer = javaFileObject.openWriter()) {
                                javaFile.writeTo(writer);
                            }
                        } catch (IOException ioException) {
                            messager.printMessage(Diagnostic.Kind.ERROR, "Error occurred generating Oracle SDK factories: " + ioException.getMessage(), e);
                        }

                    }
                }
            }
        }
        return false;
    }

    private TypeSpec.Builder defineSuperclass(String packageName, String simpleName, String factoryName) {
        final TypeSpec.Builder builder = TypeSpec.classBuilder(factoryName);
        builder.superclass(ParameterizedTypeName.get(
                ClassName.get("io.micronaut.oci.core.sdk", "AbstractSdkClientFactory"),
                ClassName.get(packageName, simpleName + ".Builder"),
                ClassName.get(packageName, simpleName))
        );
        return builder;
    }

    private MethodSpec.Builder buildConstructor(String simpleName, TypeSpec.Builder builder) {
        final MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ClassName.get("com.oracle.bmc", "ClientConfiguration"), "clientConfiguration")
                .addParameter(ParameterSpec.builder(ClassName.get("com.oracle.bmc.http", "ClientConfigurator"), "clientConfigurator")
                                .addAnnotation(Nullable.class).build())
                .addParameter(ParameterSpec.builder(ClassName.get("com.oracle.bmc.http.signing", "RequestSignerFactory"), "requestSignerFactory")
                                .addAnnotation(Nullable.class).build())
                .addCode(CodeBlock.builder()
                        .addStatement("super(" + simpleName + ".builder(), clientConfiguration, clientConfigurator, requestSignerFactory)")
                        .addStatement("builder = super.getBuilder()").build());
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        return constructor;
    }

    private List<String> resolveClientNames(Element e) {
        List<String> clientNames = new ArrayList<>();
        final List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            TypeElement te = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (Requires.class.getName().equals(te.getQualifiedName().toString())) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                    final ExecutableElement executableElement = entry.getKey();
                    if (executableElement.getSimpleName().toString().equals("classes")) {
                        final AnnotationValue value = entry.getValue();
                        final Object v = value.getValue();
                        if (v instanceof Iterable) {
                            Iterable<Object> i = (Iterable) v;
                            for (Object o : i) {
                                if (o instanceof AnnotationValue) {
                                    final Object nested = ((AnnotationValue) o).getValue();
                                    if (nested instanceof DeclaredType) {
                                        final TypeElement dte = (TypeElement) ((DeclaredType) nested).asElement();
                                        clientNames.add(dte.getQualifiedName().toString());
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return clientNames;
    }
}
