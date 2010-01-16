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

/**
 * 生成するコンテナクラスの設定情報。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class ContainerConfig {

    private AccessPolicy access;

    private String namePattern;

    /**
     * インスタンスを生成する。
     * @param access 明示的なアクセス修飾子
     * @param namePattern MessageFormatの形式で記述された生成するクラス名のパターン
     */
    public ContainerConfig(AccessPolicy access, String namePattern) {
        if (access == null) {
            throw new IllegalArgumentException("access is null"); //$NON-NLS-1$
        }
        if (namePattern == null) {
            throw new IllegalArgumentException("namePattern is null"); //$NON-NLS-1$
        }
        this.access = access;
        this.namePattern = namePattern;
    }

    /**
     * 設定によって上書きするアクセス修飾子を返す。
     * @return 設定によって上書きするアクセス修飾子
     */
    public AccessPolicy getAccessOverride() {
        return access;
    }

    /**
     * MessageFormatの形式で記述された生成するクラス名のパターンを返す。
     * @return 生成するクラス名のパターン
     */
    public String getNamePattern() {
        return namePattern;
    }
}
