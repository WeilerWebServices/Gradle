package org.gradle.test.performance96_3;

import static org.junit.Assert.*;

public class Test96_216 {
    private final Production96_216 production = new Production96_216("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}