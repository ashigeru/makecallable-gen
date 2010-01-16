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
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.EnumConstantDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeVariable;
import com.sun.mirror.util.DeclarationFilter;
import com.sun.mirror.util.SourcePosition;
import com.sun.mirror.util.Types;

/**
 * {@code MakeCallable}アノテーションが付与されたメソッドまたはコンストラクタに対し、対応する
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class MakeCallableProcessor implements AnnotationProcessor {

    private final AnnotationProcessorEnvironment environment;

    private final AnnotationTypeDeclaration containerDecl;

    private final AnnotationTypeDeclaration makecallableDecl;

    private final DeclaredType exceptionType;

    private final DeclaredType errorType;

    private final DeclaredType serializableType;

    /**
     * インスタンスを生成する。
     * @param environment 実行環境
     * @param makecallableDecl 処理対象の注釈
     * @param containerDecl
     */
    public MakeCallableProcessor(
            AnnotationProcessorEnvironment environment,
            AnnotationTypeDeclaration containerDecl,
            AnnotationTypeDeclaration makecallableDecl) {
        if (environment == null) {
            throw new IllegalArgumentException("environment is null"); //$NON-NLS-1$
        }
        if (containerDecl == null) {
            throw new IllegalArgumentException("containerDecl is null"); //$NON-NLS-1$
        }
        if (makecallableDecl == null) {
            throw new IllegalArgumentException("makecallableDecl is null"); //$NON-NLS-1$
        }
        this.environment = environment;
        this.containerDecl = containerDecl;
        this.makecallableDecl = makecallableDecl;

        this.exceptionType = getType(Exception.class);
        this.errorType = getType(Error.class);
        this.serializableType = getType(Serializable.class);
    }

    private DeclaredType getType(Class<?> aClass) {
        assert aClass != null;
        return environment.getTypeUtils().getDeclaredType(environment.getTypeDeclaration(aClass.getName()));
    }

    public void process() {
        debug(null, "{0} Start", MakeCallableProcessor.class);

        Collection<TypeDeclaration> containers = findContainers();
        debug(null, "Containers: {0}", containers);

        Map<TypeDeclaration, ContainerModel> models = new HashMap<TypeDeclaration, ContainerModel>();
        for (TypeDeclaration decl : containers) {
            ContainerModel model = toModel(decl);
            if (model != null) {
                debug(decl.getPosition(), "{0} is valid container (with {1} methods)",
                    model.getSimpleName(), model.getMethods().size());
                models.put(decl, model);
            }
        }
        for (Map.Entry<TypeDeclaration, ContainerModel> entry : models.entrySet()) {
            TypeDeclaration type = entry.getKey();
            ContainerModel model = entry.getValue();
            try {
                debug(type.getPosition(), "Generating {0}", model.getSimpleName());
                SourceGenerator.generate(environment, model);
            }
            catch (IOException e) {
                environment.getMessager().printError(
                    type.getPosition(),
                    MessageFormat.format(
                        "Cannot generate a @MakeCallable class {0} into {1}",
                        model.getSimpleName(),
                        model.getPackageName().length() == 0 ? "default package" : model.getPackageName()));
            }
        }
    }

    private Collection<TypeDeclaration> findContainers() {
        Collection<TypeDeclaration> containers = new HashSet<TypeDeclaration>();
        containers.addAll(findExplicitContainers());
        containers.addAll(findImplicitContainers());
        for (Iterator<TypeDeclaration> iter = containers.iterator(); iter.hasNext(); ) {
            TypeDeclaration container = iter.next();
            if (verify(container) == false) {
                iter.remove();
            }
        }
        return containers;
    }

    private Collection<TypeDeclaration> findExplicitContainers() {
        // 明示的に@Containerが含まれる
        return DeclarationFilter.getFilter(TypeDeclaration.class)
            .filter(environment.getDeclarationsAnnotatedWith(containerDecl), TypeDeclaration.class);
    }

    private Collection<TypeDeclaration> findImplicitContainers() {
        // @MakeCallableが付与されているメソッドを持つ
        Collection<MethodDeclaration> methods = DeclarationFilter.getFilter(MethodDeclaration.class)
            .filter(environment.getDeclarationsAnnotatedWith(makecallableDecl), MethodDeclaration.class);

        Collection<TypeDeclaration> results = new HashSet<TypeDeclaration>();
        for (MethodDeclaration method : methods) {
            TypeDeclaration declaring = method.getDeclaringType();
            results.add(declaring);
        }
        return results;
    }

    private boolean verify(TypeDeclaration container) {
        assert container != null;
        boolean verified = true;
        verified &= veifyTopLevel(container);
        return verified;
    }

    private boolean veifyTopLevel(TypeDeclaration type) {
        assert type != null;
        if (type.getDeclaringType() != null) {
            environment.getMessager().printError(type.getPosition(), MessageFormat.format(
                "The container class {0} must be top level",
                type.getSimpleName()));
            return false;
        }
        return true;
    }

    private ContainerModel toModel(TypeDeclaration container) {
        assert container != null;
        ContainerConfig config = parseContainerConfig(container);
        if (config == null) {
            return null;
        }
        List<MethodModel> methods = new ArrayList<MethodModel>();
        for (MethodDeclaration method : container.getMethods()) {
            AnnotationMirror target = getMethodAnnotation(method);
            if (target == null) {
                continue;
            }
            debug(method.getPosition(), "{0} is annotated with MakeCallable", method);
            MethodModel model = toModel(target, method);
            if (model != null) {
                debug(method.getPosition(), "{0} is valid MakeCallable method", method);
                methods.add(model);
            }
        }
        return new ContainerModel(environment.getTypeUtils(), config, container, methods);
    }

    private ContainerConfig parseContainerConfig(TypeDeclaration container) {
        AnnotationMirror annotation = findAnnotation(containerDecl, container);
        Map<String, AnnotationValue> elements = getElements(annotation, containerDecl);
        AccessPolicy acessible = getAccessibility(elements);
        String namePattern = getContainerNamePattern(elements);
        if (namePattern == null) {
            return null;
        }
        ContainerConfig config = new ContainerConfig(acessible, namePattern);
        return config;
    }

    private AnnotationMirror getMethodAnnotation(MethodDeclaration method) {
        assert method != null;
        return findAnnotation(makecallableDecl, method);
    }

    private MethodModel toModel(AnnotationMirror target, MethodDeclaration method) {
        assert target != null;
        assert method != null;
        if (verify(method) == false) {
            debug(method.getPosition(), "{0} is invalid target method", method);
            return null;
        }
        debug(method.getPosition(), "{0} is valid target method", method);

        MethodConfig config = parseMethodConfig(target);
        if (config == null) {
            debug(method.getPosition(), "{0} has invalid config", method);
            return null;
        }
        debug(method.getPosition(), "{0} is valid method", method);
        return new MethodModel(environment.getTypeUtils(), config, method);
    }

    private boolean verify(MethodDeclaration method) {
        assert method != null;
        boolean verified = true;
        verified &= verifyMethodAccess(method);
        verified &= verifyMethodThrows(method);
        return verified;
    }

    private boolean verifyMethodAccess(MethodDeclaration method) {
        assert method != null;
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            environment.getMessager().printError(method.getPosition(), MessageFormat.format(
                "The callable method \"{0}\" must not be private",
                method.getSimpleName()));
            return false;
        }
        return true;
    }

    private boolean verifyMethodThrows(MethodDeclaration method) {
        assert method != null;
        Collection<ReferenceType> unsupported = findUnsupportedExceptions(method.getThrownTypes());
        if (unsupported.isEmpty() == false) {
            environment.getMessager().printError(method.getPosition(), MessageFormat.format(
                "The callable method \"{0}\" can throw only subclass of Exception or Error: {1}",
                method.getSimpleName(),
                unsupported));
            return false;
        }
        return true;
    }

    private Collection<ReferenceType> findUnsupportedExceptions(Collection<ReferenceType> thrownTypes) {
        assert thrownTypes != null;
        Types types = environment.getTypeUtils();
        Collection<ReferenceType> results = new HashSet<ReferenceType>();
        for (ReferenceType t : thrownTypes) {
            if (t instanceof TypeVariable
                    || (types.isSubtype(t, exceptionType) == false && types.isSubtype(t, errorType) == false)) {
                results.add(t);
            }
        }
        return results;
    }

    private MethodConfig parseMethodConfig(AnnotationMirror annotation) {
        assert annotation != null;
        Map<String, AnnotationValue> elements = getElements(annotation, makecallableDecl);
        AccessPolicy access = getAccessibility(elements);
        String name = getCallableName(elements);
        if (name == null) {
            return null;
        }
        else if (name.equals(Names.MAKE_CALLABLE_NAME_DEFAULT)) {
            name = null;
        }
        List<DeclaredType> markerInterfaces = getMarkerInterfaces(elements);
        return new MethodConfig(access, name, markerInterfaces);
    }

    private AnnotationMirror findAnnotation(AnnotationTypeDeclaration annotationDecl, Declaration elementDecl) {
        assert annotationDecl != null;
        assert elementDecl != null;
        for (AnnotationMirror a : elementDecl.getAnnotationMirrors()) {
            if (annotationDecl.equals(a.getAnnotationType().getDeclaration())) {
                return a;
            }
        }
        return null;
    }

    private Map<String, AnnotationValue> getElements(
            AnnotationMirror annotation,
            AnnotationTypeDeclaration declaration) {
        assert declaration != null;
        Map<String, AnnotationValue> values = new HashMap<String, AnnotationValue>();
        if (annotation != null) {
            for (Map.Entry<AnnotationTypeElementDeclaration, AnnotationValue> entry
                    : annotation.getElementValues().entrySet()) {
                values.put(entry.getKey().getSimpleName(), entry.getValue());
            }
        }
        for (AnnotationTypeElementDeclaration element : declaration.getMethods()) {
            if (values.containsKey(element.getSimpleName()) == false) {
                values.put(element.getSimpleName(), element.getDefaultValue());
            }
        }
        return values;
    }

    private AccessPolicy getAccessibility(Map<String, AnnotationValue> elements) {
        assert elements != null;
        AnnotationValue value = elements.get(Names.COMMON_ACCESS);
        EnumConstantDeclaration constant = (EnumConstantDeclaration) value.getValue();
        return AccessPolicy.valueOf(constant.getSimpleName());
    }

    private String getContainerNamePattern(Map<String, AnnotationValue> elements) {
        assert elements != null;
        AnnotationValue value = elements.get(Names.CONTAINER_NAME_PATTERN);
        String pattern = (String) value.getValue();
        try {
            String sample = MessageFormat.format(pattern, "Class1");
            if (isJavaIdentifier(sample) == false) {
                environment.getMessager().printError(value.getPosition(), MessageFormat.format(
                    "\"{0}\" must be a valid Java name pattern (\"{1}\")",
                    Names.CONTAINER_NAME_PATTERN,
                    pattern));
                return null;
            }
            String sample2 = MessageFormat.format(pattern, "Class2");
            if (sample.equals(sample2)) {
                environment.getMessager().printWarning(value.getPosition(), MessageFormat.format(
                    "\"{0}\" should contain a parameter '{'0'}' (\"{1}\")",
                    Names.CONTAINER_NAME_PATTERN,
                    pattern));
            }
            return pattern;
        }
        catch (IllegalArgumentException e) {
            environment.getMessager().printError(value.getPosition(), MessageFormat.format(
                "\"{0}\" must be a valid MessageFormat pattern (\"{1}\")",
                Names.CONTAINER_NAME_PATTERN,
                pattern));
            return null;
        }
    }

    private String getCallableName(Map<String, AnnotationValue> elements) {
        assert elements != null;
        AnnotationValue value = elements.get(Names.MAKE_CALLABLE_NAME);
        String name = (String) value.getValue();
        if (isJavaIdentifier(name) == false) {
            environment.getMessager().printError(value.getPosition(), MessageFormat.format(
                "\"{0}\" must be a valid Java name (\"{1}\")",
                Names.MAKE_CALLABLE_NAME,
                name));
            return null;
        }
        return name;
    }

    private boolean isJavaIdentifier(String ident) {
        assert ident != null;
        if (ident.length() == 0) {
            return false;
        }
        char[] chars = ident.toCharArray();
        if (Character.isJavaIdentifierStart(chars[0]) == false) {
            return false;
        }
        for (int i = 1; i < chars.length; i++) {
            if (Character.isJavaIdentifierPart(chars[i]) == false) {
                return false;
            }
        }
        return true;
    }

    private List<DeclaredType> getMarkerInterfaces(Map<String, AnnotationValue> elements) {
        assert elements != null;
        AnnotationValue value = elements.get(Names.MAKE_CALLABLE_SERIALIZABLE);
        if (value.getValue() == Boolean.TRUE) {
            return Arrays.asList(serializableType);
        }
        return Collections.emptyList();
    }

    private void debug(SourcePosition position, String pattern, Object...arguments) {
        environment.getMessager().printNotice(position, MessageFormat.format(pattern, arguments));
    }
}
