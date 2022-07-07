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
package io.micronaut.oraclecloud.clients.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.oracle.bmc.SdkClients;
import com.oracle.bmc.graalvm.SdkClientPackages;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.micronaut.annotation.processing.AnnotationUtils;
import io.micronaut.annotation.processing.GenericUtils;
import io.micronaut.annotation.processing.ModelUtils;
import io.micronaut.annotation.processing.PublicMethodVisitor;
import io.micronaut.annotation.processing.visitor.JavaVisitorContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.inject.visitor.TypeElementVisitor;
import jakarta.inject.Singleton;

/**
 * An annotation processor that generates the Oracle Cloud SDK integration
 * for Micronaut.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@SupportedAnnotationTypes("io.micronaut.oraclecloud.clients.SdkClients")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class OracleCloudSdkProcessor extends AbstractProcessor {

    public static final String CLIENT_PACKAGE = "io.micronaut.oraclecloud.clients";

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            final Set<? extends Element> element = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element e : element) {
                List<String> clientNames = resolveClientNames(e);
                final String t = resolveClientType(e);
                final boolean isRxJava2 = t.equals("RXJAVA2");
                final boolean isReactor = t.equals("REACTOR");
                final boolean isAsync = t.equals("ASYNC");
                List<String> factoryClassNames = new ArrayList<>();
                for (String clientName : clientNames) {
                    final String packageName = NameUtils.getPackageName(clientName);
                    final String simpleName = NameUtils.getSimpleName(clientName);

                    if (isRxJava2) {
                        writeRxJava2Clients(e, packageName, simpleName);
                    } else {
                        if (isReactor) {
                            writeReactorClients(e, packageName, simpleName);
                        } else {
                            if (isAsync) {
                                factoryClassNames.add(writeClientFactory(e, packageName, simpleName));
                            }
                        }
                    }
                }

                if (!factoryClassNames.isEmpty()) {
                    try {
                        final FileObject nativeImageProps = filer.createResource(
                                StandardLocation.CLASS_OUTPUT,
                                "",
                                "META-INF/native-image/io.micronaut.oraclecloud/micronaut-oraclecloud-sdk/native-image.properties",
                                e

                        );
                        Properties properties = new Properties();
                        properties.put("Args", "--initialize-at-run-time=" + String.join(",", factoryClassNames));
                        try (Writer writer = nativeImageProps.openWriter()) {
                            properties.store(writer, "Generated Native Image Configuration");
                        }
                    } catch (IOException ioException) {
                        messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write native image config: " + ioException.getMessage());
                    }
                }
            }
        }
        return false;
    }

    private void writeRxJava2Clients(Element e, String packageName, String simpleName) {
        if (simpleName.endsWith("AsyncClient")) {

            final String rx = simpleName.replace("AsyncClient", "RxClient");
            final String rxPackageName = packageName.replace("com.oracle.bmc", CLIENT_PACKAGE + ".rxjava2");

            ClassName cn = ClassName.get(rxPackageName, rx);
            TypeSpec.Builder builder = TypeSpec.classBuilder(cn);

            ClassName clientType = ClassName.get(packageName, simpleName);
            ClassName rxSingleType = ClassName.get("io.reactivex", "Single");
            final ClassName authProviderType = ClassName.get("com.oracle.bmc.auth", "AbstractAuthenticationDetailsProvider");
            final AnnotationSpec.Builder requiresSpec =
                    AnnotationSpec.builder(Requires.class)
                            .addMember("classes", "{$T.class, $T.class}", clientType, rxSingleType)
                            .addMember("beans", "{$T.class}", authProviderType);
            builder.addAnnotation(requiresSpec.build());
            builder.addAnnotation(Singleton.class);
            builder.addModifiers(Modifier.PUBLIC);
            builder.addField(clientType, "client");
            builder.addMethod(MethodSpec.constructorBuilder()
                    .addParameter(clientType, "client")
                    .addCode("this.client = client;")
                    .build());

            TypeElement typeElement = elements.getTypeElement(clientType.reflectionName());
            if (typeElement != null) {
                ModelUtils modelUtils = new ModelUtils(elements, types) { };
                GenericUtils genericUtils = new GenericUtils(elements, types, modelUtils) { };
                AnnotationUtils annotationUtils = new AnnotationUtils(processingEnv, elements, messager, types, modelUtils, genericUtils, filer) { };
                JavaVisitorContext visitorContext = new JavaVisitorContext(
                        processingEnv,
                        messager,
                        elements,
                        annotationUtils,
                        types,
                        modelUtils,
                        genericUtils,
                        filer,
                        MutableConvertibleValues.of(new LinkedHashMap<>()),
                        TypeElementVisitor.VisitorKind.ISOLATING
                );
                typeElement.asType().accept(new PublicMethodVisitor<Object, Object>(visitorContext) {
                    @Override
                    protected void accept(DeclaredType type, Element element, Object o) {
                        ExecutableElement ee = (ExecutableElement) element;
                        List<? extends VariableElement> parameters = ee.getParameters();
                        TypeMirror returnType = ee.getReturnType();
                        if (returnType instanceof DeclaredType && parameters.size() == 2) {
                            DeclaredType dt = (DeclaredType) returnType;
                            Element e = dt.asElement();
                            if (e.getSimpleName().toString().equals("Future")) {
                                List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
                                if (typeArguments.size() == 1) {
                                    VariableElement variableElement = parameters.get(0);
                                    TypeMirror m = typeArguments.get(0);
                                    if (m instanceof DeclaredType) {

                                        String methodName = ee.getSimpleName().toString();
                                        String parameterName = variableElement.getSimpleName().toString();
                                        TypeName requestType = ClassName.get(variableElement.asType());
                                        TypeName responseType = ClassName.get(m);
                                        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                                                methodName
                                        )
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(
                                                        requestType,
                                                        parameterName
                                                )
                                                .returns(
                                                        ParameterizedTypeName.get(
                                                                rxSingleType,
                                                                responseType
                                                        )
                                                );

                                        methodBuilder.addCode(CodeBlock.builder()
                                                .addStatement("return $T.create((emitter) -> {", rxSingleType)
                                                .add("this.client." + methodName + "(" + parameterName + ",")
                                                .add("new $T<" + requestType + "," + responseType + ">(emitter)", ClassName.get("io.micronaut.oraclecloud.clients.rxjava2", "AsyncHandlerEmitter"))
                                                .addStatement(")")
                                                .addStatement("})")
                                        .build());
                                        builder.addMethod(methodBuilder.build());

                                    }
                                }
                            }
                        }

                    }
                }, null);
            }

            final JavaFile javaFile = JavaFile.builder(rxPackageName, builder.build()).build();
            try {
                final JavaFileObject javaFileObject = filer.createSourceFile(cn.reflectionName(), e);
                try (Writer writer = javaFileObject.openWriter()) {
                    javaFile.writeTo(writer);
                }
            } catch (IOException ioException) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Error occurred generating Oracle SDK factories: " + ioException.getMessage(), e);
            }
        }
    }

    private void writeReactorClients(Element e, String packageName, String simpleName) {
        if (simpleName.endsWith("AsyncClient")) {

            final String rx = simpleName.replace("AsyncClient", "ReactorClient");
            final String rxPackageName = packageName.replace("com.oracle.bmc", CLIENT_PACKAGE + ".reactor");

            ClassName cn = ClassName.get(rxPackageName, rx);
            TypeSpec.Builder builder = TypeSpec.classBuilder(cn);

            ClassName clientType = ClassName.get(packageName, simpleName);
            ClassName rxSingleType = ClassName.get("reactor.core.publisher", "Mono");
            final ClassName authProviderType = ClassName.get("com.oracle.bmc.auth", "AbstractAuthenticationDetailsProvider");
            final AnnotationSpec.Builder requiresSpec =
                    AnnotationSpec.builder(Requires.class)
                            .addMember("classes", "{$T.class, $T.class}", clientType, rxSingleType)
                            .addMember("beans", "{$T.class}", authProviderType);
            builder.addAnnotation(requiresSpec.build());
            builder.addAnnotation(Singleton.class);
            builder.addModifiers(Modifier.PUBLIC);
            builder.addField(clientType, "client");
            builder.addMethod(MethodSpec.constructorBuilder()
                                      .addParameter(clientType, "client")
                                      .addCode("this.client = client;")
                                      .build());

            TypeElement typeElement = elements.getTypeElement(clientType.reflectionName());
            if (typeElement != null) {
                ModelUtils modelUtils = new ModelUtils(elements, types) { };
                GenericUtils genericUtils = new GenericUtils(elements, types, modelUtils) { };
                AnnotationUtils annotationUtils = new AnnotationUtils(processingEnv, elements, messager, types, modelUtils, genericUtils, filer) { };
                JavaVisitorContext visitorContext = new JavaVisitorContext(
                        processingEnv,
                        messager,
                        elements,
                        annotationUtils,
                        types,
                        modelUtils,
                        genericUtils,
                        filer,
                        MutableConvertibleValues.of(new LinkedHashMap<>()),
                        TypeElementVisitor.VisitorKind.ISOLATING
                );
                typeElement.asType().accept(new PublicMethodVisitor<Object, Object>(visitorContext) {
                    @Override
                    protected void accept(DeclaredType type, Element element, Object o) {
                        ExecutableElement ee = (ExecutableElement) element;
                        List<? extends VariableElement> parameters = ee.getParameters();
                        TypeMirror returnType = ee.getReturnType();
                        if (returnType instanceof DeclaredType && parameters.size() == 2) {
                            DeclaredType dt = (DeclaredType) returnType;
                            Element e = dt.asElement();
                            if (e.getSimpleName().toString().equals("Future")) {
                                List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
                                if (typeArguments.size() == 1) {
                                    VariableElement variableElement = parameters.get(0);
                                    TypeMirror m = typeArguments.get(0);
                                    if (m instanceof DeclaredType) {

                                        String methodName = ee.getSimpleName().toString();
                                        String parameterName = variableElement.getSimpleName().toString();
                                        TypeName requestType = ClassName.get(variableElement.asType());
                                        TypeName responseType = ClassName.get(m);
                                        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                                                methodName
                                        )
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(
                                                        requestType,
                                                        parameterName
                                                )
                                                .returns(
                                                        ParameterizedTypeName.get(
                                                                rxSingleType,
                                                                responseType
                                                        )
                                                );

                                        methodBuilder.addCode(CodeBlock.builder()
                                                                      .addStatement("return $T.create((sink) -> {", rxSingleType)
                                                                      .add("this.client." + methodName + "(" + parameterName + ",")
                                                                      .add("new $T<" + requestType + "," + responseType + ">(sink)", ClassName.get("io.micronaut.oraclecloud.clients.reactor", "AsyncHandlerSink"))
                                                                      .addStatement(")")
                                                                      .addStatement("})")
                                                                      .build());
                                        builder.addMethod(methodBuilder.build());

                                    }
                                }
                            }
                        }

                    }
                }, null);
            }

            final JavaFile javaFile = JavaFile.builder(rxPackageName, builder.build()).build();
            try {
                final JavaFileObject javaFileObject = filer.createSourceFile(cn.reflectionName(), e);
                try (Writer writer = javaFileObject.openWriter()) {
                    javaFile.writeTo(writer);
                }
            } catch (IOException ioException) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Error occurred generating Oracle SDK factories: " + ioException.getMessage(), e);
            }
        }
    }

    private String writeClientFactory(Element e, String packageName, String simpleName) {
        final String factoryName = simpleName + "Factory";
        final String factoryPackageName = packageName.replace("com.oracle.bmc", CLIENT_PACKAGE);
        final TypeSpec.Builder builder = defineSuperclass(packageName, simpleName, factoryName);
        final MethodSpec.Builder constructor = buildConstructor(simpleName, builder);
        final ClassName builderType = ClassName.get(packageName, simpleName + ".Builder");
        builder.addField(FieldSpec.builder(builderType, "builder", Modifier.PRIVATE).build());
        builder.addAnnotation(Factory.class);
        final ClassName authProviderType = ClassName.get("com.oracle.bmc.auth", "AbstractAuthenticationDetailsProvider");
        final AnnotationSpec.Builder requiresSpec = AnnotationSpec.builder(Requires.class)
                .addMember("classes", simpleName + ".class")
                .addMember("beans", authProviderType.canonicalName() + ".class");
        final AnnotationSpec.Builder preDestroy = AnnotationSpec.builder(Bean.class)
                .addMember("preDestroy", CodeBlock.of("\"close\""));


        builder.addAnnotation(requiresSpec.build());
        // bit of a hack this but not sure of a better way
        final boolean isBootstrapCompatible = simpleName.equals("SecretsClient") || simpleName.equals("VaultsClient");
        if (isBootstrapCompatible) {
            builder.addAnnotation(BootstrapContextCompatible.class);
        }
        builder.addMethod(constructor.build());

        final MethodSpec.Builder getBuilder = MethodSpec.methodBuilder("getBuilder");
        getBuilder.returns(builderType)
                .addAnnotation(Singleton.class)
                .addAnnotation(requiresSpec.build())
                .addModifiers(Modifier.PROTECTED)
                .addCode("return super.getBuilder();");
        if (isBootstrapCompatible) {
            getBuilder.addAnnotation(BootstrapContextCompatible.class);
        }
        builder.addMethod(getBuilder.build());

        final MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build");

        buildMethod.returns(ClassName.get(packageName, simpleName))
                .addParameter(authProviderType, "authenticationDetailsProvider")
                .addAnnotation(Singleton.class)
                .addAnnotation(requiresSpec.build())
                .addAnnotation(preDestroy.build())
                .addModifiers(Modifier.PROTECTED)
                .addCode("return builder.build(authenticationDetailsProvider);");
        if (isBootstrapCompatible) {
            buildMethod.addAnnotation(BootstrapContextCompatible.class);
        }
        builder.addMethod(buildMethod.build());


        final JavaFile javaFile = JavaFile.builder(factoryPackageName, builder.build()).build();
        final String factoryQualifiedClassName = factoryPackageName + "." + factoryName;
        try {
            final JavaFileObject javaFileObject = filer.createSourceFile(factoryQualifiedClassName, e);
            try (Writer writer = javaFileObject.openWriter()) {
                javaFile.writeTo(writer);
            }
        } catch (IOException ioException) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error occurred generating Oracle SDK factories: " + ioException.getMessage(), e);
        }
        return factoryQualifiedClassName;
    }

    private TypeSpec.Builder defineSuperclass(String packageName, String simpleName, String factoryName) {
        final TypeSpec.Builder builder = TypeSpec.classBuilder(factoryName);
        builder.superclass(ParameterizedTypeName.get(
                ClassName.get("io.micronaut.oraclecloud.core.sdk", "AbstractSdkClientFactory"),
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
        return resolveOraceCloudClientNamesFromGraalVmAddons();
    }

    private List<String> resolveOraceCloudClientNamesFromGraalVmAddons() {
        List<String> results = new ArrayList<>();
        Class<?> metadataClass = ClassUtils.forName("com.oracle.bmc.graalvm.SdkAutomaticFeatureMetadata", getClass().getClassLoader()).orElse(null);
        if (metadataClass != null) {
            SdkClientPackages allSdkClientPackages =
                    metadataClass.getAnnotation(SdkClientPackages.class);
            for (Class<?> sdkClientsMetadataPath : allSdkClientPackages.value()) {
                SdkClients declaredClients = sdkClientsMetadataPath.getDeclaredAnnotation(SdkClients.class);
                if (declaredClients != null) {
                    Class<?>[] allSdkClients =
                            declaredClients.value();
                    for (Class<?> sdkClient : allSdkClients) {
                        results.add(sdkClient.getName());
                    }
                }
            }
        }
        return results;
    }

    private String resolveClientType(Element e) {
        final List<? extends AnnotationMirror> annotationMirrors = e.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            TypeElement te = (TypeElement) annotationMirror.getAnnotationType().asElement();
            String ann = te.getSimpleName().toString();
            if (ann.equals("SdkClients")) {
                final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
                final Iterator<? extends AnnotationValue> i = values.values().iterator();
                if (i.hasNext()) {
                    final AnnotationValue av = i.next();
                    final Object v = av.getValue();
                    if (v != null) {
                        return v.toString();
                    }
                }
            }
        }
        return "ASYNC";
    }
}
