/*
 * Copyright 2010 @ashigeru.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.ashigeru.lab.makecallable.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.PrimitiveType;
import com.sun.mirror.type.TypeMirror;

/**
 * 実際にソースプログラムを生成する。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class SourceGenerator {

    private static final String INDENT_UNIT = "    ";

    private static final String THIS = "__this__";

    private static final String RETURN_TYPE_VAR = "R";

    private AnnotationProcessorEnvironment environment;

    private SourceWriter out;

    private ContainerModel container;

    private SourceGenerator(AnnotationProcessorEnvironment environment, SourceWriter out, ContainerModel container) {
        assert environment != null;
        assert out != null;
        assert container != null;
        this.environment = environment;
        this.out = out;
        this.container = container;
    }

    /**
     * 指定の出力先に、指定のコンテナを出力する。
     * @param environment 環境オブジェクト
     * @param container 出力するコンテナ
     * @throws IOException 出力に失敗した場合
     */
    public static void generate(
            AnnotationProcessorEnvironment environment,
            ContainerModel container) throws IOException {
        if (environment == null) {
            throw new IllegalArgumentException("environment is null"); //$NON-NLS-1$
        }
        if (container == null) {
            throw new IllegalArgumentException("container is null"); //$NON-NLS-1$
        }
        PrintWriter out = open(environment, container);
        try {
            SourceGenerator generator = new SourceGenerator(environment, new SourceWriter(out), container);
            generator.generateHeadComments();
            generator.generatePackageDecl();
            generator.generateContainer();
        }
        finally {
            out.close();
        }
    }

    private void generateHeadComments() {
        List<String> headComments = getHeadComments();
        if (headComments.isEmpty()) {
            return;
        }
        out.line("/*");
        out.begin(" * ");
        for (String line : headComments) {
            out.line(line);
        }
        out.end();
        out.line(" */");
    }

    private void generatePackageDecl() {
        String packageName = container.getPackageName();
        if (packageName != null) {
            out.line("package ${[0]};", packageName);
        }
    }

    private List<String> getHeadComments() {
        // TODO コメントを入れたい
        return Collections.emptyList();
    }

    private void generateContainer() {
        out.line("${[0] }class ${[1]}${<[2]>} {",
            container.getAccess(),
            container.getSimpleName(),
            container.getTypeParameters());
        out.begin(INDENT_UNIT);
        generateContainerBody();
        out.end();
        out.line("}");
    }

    private void generateContainerBody() {
        generateThisField();
        generateContainerConstructor();
        List<MethodModel> methods = container.getMethods();
        for (MethodModel method : methods) {
            generateDelegate(method);
        }
        for (MethodModel method : methods) {
            generateCallable(method);
        }
    }

    private void generateThisField() {
        out.line("private ${[0]} ${[1]};", container.getType(), THIS);
    }

    private void generateContainerConstructor() {
        out.line("public ${[0]}(${[1]} target) {", container.getSimpleName(), container.getType());
        out.begin(INDENT_UNIT);
        out.line("this.${[0]} = target;", THIS);
        out.end();
        out.line("}");
    }

    private void generateDelegate(MethodModel method) {
        assert method != null;
        out.line("${[0] }${[1] }${<[2]> }${[3]}<${[4]}> ${[5]}(${[6]})${ throws [7]} {",
            method.getAccess(),
            method.isStatic() ? "static" : null,
            method.getTypeParameters(),
            method.getName(),
            boxing(method.getReturnType()),
            method.getTargetName(),
            method.getParameters(),
            method.getExceptionTypes());
        out.begin(INDENT_UNIT);
        generateDelegateBody(method);
        out.end();
        out.line("}");
    }

    private void generateDelegateBody(MethodModel method) {
        assert method != null;
        if (method.isStatic()) {
            out.line("return new ${[0]}<${[1]}>(${[3]});",
                method.getName(),
                boxing(method.getReturnType()),
                THIS,
                toParameterNames(method.getParameters()));
        }
        else {
            out.line("return new ${[0]}<${[1]}>(this.${[2]}${, [3]});",
                method.getName(),
                boxing(method.getReturnType()),
                THIS,
                toParameterNames(method.getParameters()));
        }
    }

    private void generateCallable(MethodModel method) {
        assert method != null;
        out.line("public static class ${[0]}<${[1]}> implements java.util.concurrent.Callable<${[1]}>${, [2]} {",
            method.getName(),
            RETURN_TYPE_VAR,
            method.getExtraMarkerInterfaces());
        out.begin(INDENT_UNIT);
        generateCallableBody(method);
        out.end();
        out.line("}");
    }

    private void generateCallableBody(MethodModel method) {
        assert method != null;
        generateCallableFields(method);
        generateConstructor(method);
        generateCallMethod(method);
    }

    private void generateCallableFields(MethodModel method) {
        assert method != null;
        out.line("private static final long serialVersionUID = ${[0]}L;", calculateHash(method));
        if (method.isStatic() == false) {
            out.line("private ${[0]} ${[1]};", erase(container.getType()), THIS);
        }
        for (String parameter : erase(method.getParameters())) {
            out.line("private ${[0]};", parameter);
        }
    }

    private void generateConstructor(MethodModel method) {
        assert method != null;
        if (method.isStatic()) {
            out.line("${[0]}(${[3]}) {",
                method.getName(),
                erase(container.getType()),
                THIS,
                erase(method.getParameters()));
        }
        else {
            out.line("${[0]}(${[1]} ${[2]} ${, [3]}) {",
                method.getName(),
                erase(container.getType()),
                THIS,
                erase(method.getParameters()));
        }
        out.begin(INDENT_UNIT);
        if (method.isStatic() == false) {
            out.line("this.${[0]} = ${[0]};", THIS);
        }
        for (int i = 0, n = method.getParameters().size(); i < n; i++) {
            out.line("this.${[0]} = ${[0]};", argumentNameOf(i));
        }
        out.end();
        out.line("}");
    }

    private void generateCallMethod(MethodModel method) {
        assert method != null;
        out.line("public ${[0]} call()${ throws [1]} {", RETURN_TYPE_VAR, method.getExceptionTypes());
        out.begin(INDENT_UNIT);
        if (method.isVoid()) {
            out.line("${[0]}.${[1]}(${[2]});",
                method.isStatic() ? erase(container.getType()) : THIS,
                method.getTargetName(),
                generateArgumentNames(method.getParameters()));
            out.line("return null;");
        }
        else {
            out.line("return (${[3]}) ${([4]) }${[0]}.${[1]}(${[2]});",
                method.isStatic() ? erase(container.getType()) : THIS,
                method.getTargetName(),
                generateArgumentNames(method.getParameters()),
                RETURN_TYPE_VAR,
                boxingIfPrimitive(method.getReturnType()));
        }
        out.end();
        out.line("}");
    }

    private List<String> toParameterNames(Collection<ParameterDeclaration> parameters) {
        assert parameters != null;
        List<String> results = new ArrayList<String>();
        for (ParameterDeclaration p : parameters) {
            results.add(p.getSimpleName());
        }
        return results;
    }

    private List<String> generateArgumentNames(Collection<ParameterDeclaration> parameters) {
        assert parameters != null;
        List<String> results = new ArrayList<String>();
        for (int i = 0, n = parameters.size(); i < n; i++) {
            results.add(argumentNameOf(i));
        }
        return results;
    }

    private TypeMirror erase(TypeMirror t) {
        return environment.getTypeUtils().getErasure(t);
    }

    private Collection<String> erase(Collection<ParameterDeclaration> parameters) {
        Collection<String> results = new ArrayList<String>();
        int index = 0;
        for (ParameterDeclaration p : parameters) {
            results.add(String.format("%s %s", erase(p.getType()), argumentNameOf(index++)));
        }
        return results;
    }

    private String argumentNameOf(int index) {
        assert index >= 0;
        return String.format("a%d", index);
    }

    private TypeMirror boxingIfPrimitive(TypeMirror t) {
        assert t != null;
        if ((t instanceof PrimitiveType) == false) {
            return null;
        }
        return boxing(t);
    }

    private TypeMirror boxing(TypeMirror t) {
        assert t != null;
        if (environment.getTypeUtils().getVoidType().equals(t)) {
            return getType(Void.class);
        }
        if ((t instanceof PrimitiveType) == false) {
            return t;
        }
        PrimitiveType p = (PrimitiveType) t;
        switch (p.getKind()) {
        case BOOLEAN:
            return getType(Boolean.class);
        case BYTE:
            return getType(Byte.class);
        case CHAR:
            return getType(Character.class);
        case DOUBLE:
            return getType(Double.class);
        case FLOAT:
            return getType(Float.class);
        case INT:
            return getType(Integer.class);
        case LONG:
            return getType(Long.class);
        case SHORT:
            return getType(Short.class);
        default:
            throw new AssertionError(p);
        }
    }

    private DeclaredType getType(Class<?> runtime) {
        String name = runtime.getName();
        TypeDeclaration type = environment.getTypeDeclaration(name);
        return environment.getTypeUtils().getDeclaredType(type);
    }

    private long calculateHash(MethodModel method) {
        assert method != null;
        long result = 0;
        if (method.isStatic() == false) {
            result++;
            result += erase(container.getType()).toString().hashCode();
        }
        for (ParameterDeclaration p : method.getParameters()) {
            result *= 31;
            result += erase(p.getType()).toString().hashCode();
        }
        return result;
    }

    private static PrintWriter open(AnnotationProcessorEnvironment environment, ContainerModel model) throws IOException {
        assert environment != null;
        assert model != null;
        String fqn;
        if (model.getPackageName().length() >= 1) {
            fqn = model.getPackageName() + "." + model.getSimpleName();
        }
        else {
            fqn = model.getSimpleName();
        }

        Filer filer = environment.getFiler();
        return filer.createSourceFile(fqn);
    }
}
