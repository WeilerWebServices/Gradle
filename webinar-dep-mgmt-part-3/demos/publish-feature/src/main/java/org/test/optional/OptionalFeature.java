package org.test.optional;

import org.apache.commons.lang3.StringUtils;

public class OptionalFeature {
    public static int lastIndexOf(String source, String searched) {
        return StringUtils.lastIndexOf(source, searched);
    }
}