package org.gradle.test.performance20_3;

import static org.junit.Assert.*;

public class Test20_216 {
    private final Production20_216 production = new Production20_216("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}