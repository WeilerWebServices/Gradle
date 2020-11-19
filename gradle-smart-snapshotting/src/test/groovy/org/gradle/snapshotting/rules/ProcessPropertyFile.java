package org.gradle.snapshotting.rules;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.files.FileishWithContents;
import org.gradle.snapshotting.operations.Operation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

public class ProcessPropertyFile<C extends Context> extends Rule<FileishWithContents, C> {
    private static final Pattern PROPERTY_FILE = Pattern.compile(".*\\.properties");
    private final Set<String> ignoredKeys;
    private final Charset encoding;

    public ProcessPropertyFile(Class<C> contextType, Set<String> ignoredKeys, Charset encoding) {
        super(FileishWithContents.class, contextType, PROPERTY_FILE);
        this.ignoredKeys = ignoredKeys;
        this.encoding = encoding;
    }

    @Override
    public void process(FileishWithContents file, C context, List<Operation> operations) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(file.open(), encoding)) {
            properties.load(reader);
        }
        List<String> propertyNames = Lists.newArrayList(properties.stringPropertyNames());
        Collections.sort(propertyNames);
        Hasher hasher = Hashing.md5().newHasher();
        for (String propertyName : propertyNames) {
            if (ignoredKeys.contains(propertyName)) {
                continue;
            }
            String value = properties.getProperty(propertyName);
            hasher.putString(propertyName, Charsets.UTF_8);
            hasher.putString(value, Charsets.UTF_8);
        }
        context.recordSnapshot(file, hasher.hash());
    }
}
