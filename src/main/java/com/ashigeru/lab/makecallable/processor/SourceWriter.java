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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ソースプログラムを出力する。
 * <p>
 * 特別な指定がない限り、すべてのメソッドは{@code null}が渡された際に{@code IllegalArgumentException}をスローする。
 * </p>
 * @author ashigeru
 */
public class SourceWriter {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{(.*?)\\[(.*?)\\](.*?)\\}"); //$NON-NLS-1$

    private PrintWriter out;

    private LinkedList<String> lineHead;

    /**
     * インスタンスを生成する。
     * @param out 出力先
     */
    public SourceWriter(PrintWriter out) {
        if (out == null) {
            throw new IllegalArgumentException("out is null"); //$NON-NLS-1$
        }
        this.out = out;
        this.lineHead = new LinkedList<String>();
    }

    /**
     * このオブジェクトの実際の出力先に対し、指定のパターンと引数からなる行を出力する。
     * <p>
     * TODO パターンの形式
     * </p>
     * @param pattern パターン文字列
     * @param arguments パターンに対する引数一覧
     */
    public void line(String pattern, Object...arguments) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern is null"); //$NON-NLS-1$
        }
        if (arguments == null) {
            throw new IllegalArgumentException("arguments is null"); //$NON-NLS-1$
        }
        for (String s : lineHead) {
            out.print(s);
        }
        out.print(apply(pattern, arguments));
        out.println();
    }

    private CharSequence apply(String pattern, Object[] arguments) {
        StringBuilder buf = new StringBuilder();
        Matcher m = PATTERN.matcher(pattern);
        int start = 0;
        while (m.find(start)) {
            buf.append(pattern.substring(start, m.start()));
            int index = Integer.parseInt(m.group(2));
            String replace = toString(arguments[index]);
            if (replace != null) {
                buf.append(m.group(1));
                buf.append(replace);
                buf.append(m.group(3));
            }
            start = m.end();
        }
        buf.append(pattern.substring(start));
        return buf;
    }

    private String toString(Object object) {
        if (object == null) {
            return null;
        }
        else if (object instanceof Collection<?>) {
            Collection<?> c = (Collection<?>) object;
            Iterator<?> iter = c.iterator();
            if (iter.hasNext() == false) {
                return null;
            }
            StringBuilder buf = new StringBuilder();
            buf.append(iter.next());
            while (iter.hasNext()) {
                buf.append(", ");
                buf.append(iter.next());
            }
            return buf.toString();
        }
        else {
            String string = String.valueOf(object);
            if (string.length() == 0) {
                return null;
            }
            return string;
        }
    }

    /**
     * 指定の接頭辞を持つブロックを開始する。
     * <p>
     * 以後、現在のブロックに対する{@link #end()}が起動されるまで、
     * {@link #line(String, Object...)}による出力の手前にこれまでに設定した接頭辞の一覧と、
     * 今回設定した接頭辞が順に付与される。
     * </p>
     * @param head ブロックの接頭辞
     */
    public void begin(String head) {
        if (head == null) {
            throw new IllegalArgumentException("head is null"); //$NON-NLS-1$
        }
        lineHead.addLast(head);
    }

    /**
     * {@link #begin(String)}によって開始されたブロックを終了する。
     * <p>
     * ブロックはスタック状に積み上げられており、まだこのメソッドが起動されていない最後の
     * {@link #begin(String)}によって開始されたブロックを終了する。
     * そのようなブロックがひとつも存在しない場合、この起動は失敗する。
     * </p>
     * @throws IllegalStateException 対象のブロックが存在しない場合
     */
    public void end() {
        if (lineHead.isEmpty()) {
            throw new IllegalStateException();
        }
        lineHead.removeLast();
    }
}
