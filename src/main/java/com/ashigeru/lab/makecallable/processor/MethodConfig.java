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

import java.util.List;

import com.sun.mirror.type.DeclaredType;

/**
 * 生成するメソッドおよび{@code Callable}インターフェースの実装に関する設定。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class MethodConfig {

    private AccessPolicy access;

    private String name;

    private List<DeclaredType> markerInterfaces;

    /**
     * インスタンスを生成する。
     * @param access 設定によって上書きするアクセス修飾子
     * @param name 設定によって上書きするメソッド単純名、既定値を利用する場合は{@code null}
     * @param markerInterfaces 実装に付与するマーカーインターフェースの一覧
     */
    public MethodConfig(AccessPolicy access, String name, List<DeclaredType> markerInterfaces) {
        if (markerInterfaces == null) {
            throw new IllegalArgumentException("markerInterfaces is null"); //$NON-NLS-1$
        }
        if (access == null) {
            throw new IllegalArgumentException("access is null"); //$NON-NLS-1$
        }
        this.access = access;
        this.name = name;
        this.markerInterfaces = markerInterfaces;
    }

    /**
     * 設定によって上書きするアクセス修飾子を返す。
     * @return 設定によって上書きするアクセス修飾子
     */
    public AccessPolicy getAccessOverride() {
        return access;
    }

    /**
     * 設定によって上書きするメソッドおよび実装クラスの単純名を返す。
     * @return 設定によって上書きする単純名、既定値を利用する場合は{@code null}
     */
    public String getNameOverride() {
        return name;
    }

    /**
     * 生成する実装クラスに付与するマーカーインターフェースの一覧を返す。
     * <p>
     * {@code Callable}インターフェース自体はここに含まれない。
     * 主に{@code Serializable}などのインターフェースを指定することを想定している。
     * </p>
     * @return 実装クラスに付与するマーカーインターフェースの一覧
     */
    public List<DeclaredType> getExtraMarkerInterfaces() {
        return markerInterfaces;
    }
}
