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

import java.util.Collection;
import java.util.List;

import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.Types;

/**
 * {@code MakeCallable}
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class MethodModel {

    private Types types;

    private MethodConfig config;

    private MethodDeclaration decl;

    /**
     * インスタンスを生成する。
     * @param types
     * @param config
     * @param decl
     */
    public MethodModel(Types types, MethodConfig config, MethodDeclaration decl) {
        super();
        this.types = types;
        this.config = config;
        this.decl = decl;
    }

    /**
     * 生成するデリゲートメソッドのアクセス性に関する修飾子を返す。
     * <p>
     * なお、生成する実装クラスは常に{@code public}である。
     * </p>
     * @return 生成するデリゲートメソッドのアクセス性に関する修飾子(public, protected, or null (package default))
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
        else if (decl.getModifiers().contains(Modifier.PROTECTED)) {
            return Modifier.PROTECTED;
        }
        return null;
    }

    /**
     * 生成するデリゲートメソッドの仮型引数一覧を返す。
     * @return 生成するデリゲートメソッドの仮型引数一覧
     */
    public Collection<TypeParameterDeclaration> getTypeParameters() {
        return decl.getFormalTypeParameters();
    }

    /**
     * 生成するデリゲートメソッドの戻り値型を返す。
     * @return 生成するデリゲートメソッドの戻り値型
     */
    public TypeMirror getReturnType() {
        return decl.getReturnType();
    }

    /**
     * 生成するデリゲートメソッドの名前を返す。
     * @return 生成するデリゲートメソッドの名前
     */
    public String getName() {
        String name = config.getNameOverride();
        if (name != null) {
            return name;
        }
        return decl.getSimpleName();
    }

    /**
     * 実際に起動されるメソッドの名前を返す。
     * @return 実際に起動されるメソッドの名前
     */
    public Object getTargetName() {
        return decl.getSimpleName();
    }

    /**
     * 生成するデリゲートメソッドの引数一覧を返す。
     * @return 生成するデリゲートメソッドの引数一覧
     */
    public Collection<ParameterDeclaration> getParameters() {
        return decl.getParameters();
    }

    /**
     * 生成するデリゲートメソッドの例外一覧を返す。
     * @return 生成するデリゲートメソッドの例外一覧
     */
    public Collection<ReferenceType> getExceptionTypes() {
        return decl.getThrownTypes();
    }

    /**
     * 生成する実装クラスに付与するマーカーインターフェースの一覧を返す。
     * @return 生成する実装クラスに付与するマーカーインターフェースの一覧
     */
    public List<DeclaredType> getExtraMarkerInterfaces() {
        return config.getExtraMarkerInterfaces();
    }

    /**
     * 起動対象がクラスメソッドである場合のみ{@code true}を返す。
     * @return 起動対象がクラスメソッドである場合のみ{@code true}
     */
    public boolean isStatic() {
        return decl.getModifiers().contains(Modifier.STATIC);
    }

    /**
     * 起動対象が戻り値を持たない場合のみ{@code true}を返す。
     * @return 起動対象が戻り値を持たない場合のみ{@code true}
     */
    public boolean isVoid() {
        return types.getVoidType().equals(decl.getReturnType());
    }
}
