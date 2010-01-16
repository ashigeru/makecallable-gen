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

import java.util.Map;

import com.sun.mirror.apt.AnnotationProcessorEnvironment;

/**
 * オプション引数。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @version $Date$
 * @author Suguru ARAKAWA
 */
public enum Options {

    /**
     * エンコーディング情報 (javac組み込みオプション)。
     */
    ENCODING("encoding", false)
    ;

    private String optionName;

    private boolean processorSpecific;

    private Options(String optionName, boolean processorSpecific) {
        assert optionName != null;
        this.optionName = optionName;
        this.processorSpecific = processorSpecific;
    }

    /**
     * このオプションの名称を返す。
     * @return このオプションの名称
     */
    public String getOptionName() {
        return this.optionName;
    }

    /**
     * このオプションの値を返す。
     * @param environment 実行環境
     * @return 対応する値、不明の場合は{@code null}
     */
    public String getOption(AnnotationProcessorEnvironment environment) {
        if (environment == null) {
            throw new IllegalArgumentException("environment is null"); //$NON-NLS-1$
        }
        Map<String, String> options = environment.getOptions();
        if (processorSpecific) {
            return options.get(optionName);
        }
        else {
            return options.get("-" + optionName);
        }
    }
}
