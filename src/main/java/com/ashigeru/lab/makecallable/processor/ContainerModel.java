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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.type.TypeVariable;
import com.sun.mirror.util.Types;

/**
 * {@link MethodModel}を束ねるコンテナクラス。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class ContainerModel {

    private Types types;

    private ContainerConfig config;

    private TypeDeclaration decl;

    private List<MethodModel> methods;

    /**
     * インスタンスを生成する。
     * @param types 型に関するユーティリティ
     * @param config このコンテナの設定
     * @param decl このコンテナの元になったクラス
     * @param methods コンテナが含むメソッドの一覧
     */
    public ContainerModel(Types types, ContainerConfig config, TypeDeclaration decl, List<MethodModel> methods) {
        if (types == null) {
            throw new IllegalArgumentException("types is null"); //$NON-NLS-1$
        }
        if (config == null) {
            throw new IllegalArgumentException("config is null"); //$NON-NLS-1$
        }
        if (decl == null) {
            throw new IllegalArgumentException("decl is null"); //$NON-NLS-1$
        }
        if (methods == null) {
            throw new IllegalArgumentException("methods is null"); //$NON-NLS-1$
        }
        this.types = types;
        this.config = config;
        this.decl = decl;
        this.methods = methods;
    }

    /**
     * 生成するコンテナのアクセス性に関する修飾子を返す。
     * @return 生成するコンテナのアクセス性に関する修飾子(public, or null (package default))
     */
    public Modifier getAccess() {
        AccessPolicy access = config.getAccessOverride();
        if (access == AccessPolicy.PUBLIC) {
            return Modifier.PUBLIC;
        }
        if (access == AccessPolicy.PACKAGE) {
            return null;
        }
        if (decl.getModifiers().contains(Modifier.PUBLIC)) {
            return Modifier.PUBLIC;
        }
        return null;
    }

    /**
     * 生成するコンテナのクラス単純名を返す。
     * @return 生成するコンテナのクラス単純名
     */
    public String getSimpleName() {
        String pattern = config.getNamePattern();
        return MessageFormat.format(pattern, decl.getSimpleName());
    }

    /**
     * 生成するコンテナの仮型引数の一覧を返す。
     * @return 生成するコンテナの仮型引数の一覧
     */
    public Collection<TypeParameterDeclaration> getTypeParameters() {
        return decl.getFormalTypeParameters();
    }

    /**
     * コンテナの元になるクラスの自然な型表現を返す。
     * <p>
     * 自然な型表現(造語)とは、クラスが総称クラスとして宣言されている場合にパラメータ化型としてそれぞれの型変数を指定したものである。
     * たとえば、{@code class Hoge<A, B extends Foo, C extends B>}という総称クラスに対して、{@code Hoge<A, B, C>}が該当する。
     * </p>
     * <p>
     * クラスが総称クラスとして宣言されていない場合は、そのクラスに対する型そのものである。
     * </p>
     * @return コンテナの元になるクラスの自然な型表現
     */
    public TypeMirror getType() {
        if (decl.getFormalTypeParameters().isEmpty()) {
            return types.getDeclaredType(decl);
        }
        List<TypeVariable> typeArgs = new ArrayList<TypeVariable>();
        for (TypeParameterDeclaration tp : decl.getFormalTypeParameters()) {
            typeArgs.add(types.getTypeVariable(tp));
        }
        DeclaredType declaredType = types.getDeclaredType(
            decl, typeArgs.toArray(new TypeVariable[typeArgs.size()]));
        return declaredType;
    }

    /**
     * このコンテナに含まれるメソッドモデルの一覧を返す。
     * @return このコンテナに含まれるメソッドモデルの一覧
     */
    public List<MethodModel> getMethods() {
        return methods;
    }

    /**
     * 生成するコンテナクラスのパッケージ名を返す。
     * @return 生成するコンテナクラスのパッケージ名、無名パッケージの場合は空文字列
     */
    public String getPackageName() {
        return decl.getPackage().getQualifiedName();
    }
}
