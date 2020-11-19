/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.std;

import com.google.common.base.Splitter;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractSourceGenerator {
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[.\\-_]");
    protected final Writer writer;
    private final String ln = System.getProperty("line.separator", "\n");

    public AbstractSourceGenerator(Writer writer) {
        this.writer = writer;
    }

    static String toJavaName(String alias) {
        return Splitter.on(SEPARATOR_PATTERN)
            .splitToList(alias)
            .stream()
            .map(StringUtils::capitalize)
            .collect(Collectors.joining());
    }

    protected void addImport(String clazz) throws IOException {
        writeLn("import " + clazz + ";");
    }

    protected void writeLn() throws IOException {
        writer.write(ln);
    }

    public void writeLn(String source) throws IOException {
        writer.write(source + ln);
    }
}
