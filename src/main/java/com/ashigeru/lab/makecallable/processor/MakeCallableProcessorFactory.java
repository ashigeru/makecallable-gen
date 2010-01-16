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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.apt.AnnotationProcessors;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

/**
 * {@link MakeCallableProcessor}を生成するファクトリ。
 * @author ashigeru
 */
public class MakeCallableProcessorFactory implements AnnotationProcessorFactory {

    public Collection<String> supportedAnnotationTypes() {
        return Arrays.asList(new String[] {
            Names.MAKE_CALLABLE,
            Names.CONTAINER,
        });
    }

    public Collection<String> supportedOptions() {
        return Arrays.asList(new String[] {
        });
    }

    public AnnotationProcessor getProcessorFor(
            Set<AnnotationTypeDeclaration> annotations,
            AnnotationProcessorEnvironment environment) {
        assert annotations != null;
        assert environment != null;
        AnnotationTypeDeclaration makeCallableDecl = find(Names.MAKE_CALLABLE, annotations);
        if (makeCallableDecl == null) {
            return AnnotationProcessors.NO_OP;
        }
        AnnotationTypeDeclaration containerDecl = find(Names.CONTAINER, annotations);
        if (containerDecl == null) {
            containerDecl = (AnnotationTypeDeclaration) environment.getTypeDeclaration(Names.CONTAINER);
        }
        return new MakeCallableProcessor(environment, containerDecl, makeCallableDecl);
    }

    private AnnotationTypeDeclaration find(String qualifiedName, Set<AnnotationTypeDeclaration> annotations) {
        assert qualifiedName != null;
        assert annotations != null;
        for (AnnotationTypeDeclaration decl : annotations) {
            if (decl.getQualifiedName().equals(qualifiedName)) {
                return decl;
            }
        }
        return null;
    }
}
