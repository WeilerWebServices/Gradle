package org.gradle.test.performance31_1;

import static org.junit.Assert.*;

public class Test31_98 {
    private final Production31_98 production = new Production31_98("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}