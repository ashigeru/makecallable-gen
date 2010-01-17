/*
 * Copyright 2009 the Seasar Foundation and the Others.
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
 * シンボルの名称に関する規則。
 * @version $Date: 2009-11-21 23:34:28 +0900 (土, 21 11 2009) $
 * @author Suguru ARAKAWA
 */
public class Names {

    /**
     * 基本パッケージのルート。
     */
    public static final String BASE_PACKAGE = "com.ashigeru.lab.makecallable";

    /**
     * このジェネレータがトリガとする注釈の限定名。
     */
    public static final String MAKE_CALLABLE = BASE_PACKAGE + ".MakeCallable";

    /**
     * コンテナの注釈の限定名。
     */
    public static final String CONTAINER = MAKE_CALLABLE + ".Container";

    /**
     * 生成するデリゲートクラスやコンテナクラスのアクセス修飾子を指定するプロパティ名。
     */
    public static final String COMMON_ACCESS = "accessible";

    /**
     * 生成するデリゲートクラスやコンテナクラスの名前を指定するプロパティ名。
     */
    public static final String COMMON_NAME_PATTERN = "name";

    /**
     * 生成するデリゲートクラスの直列化可能性を指定するプロパティ名。
     */
    public static final String MAKE_CALLABLE_SERIALIZABLE = "serializable";

    /**
     * インスタンス生成の禁止。
     */
    private Names() {
        throw new AssertionError();
    }
}
