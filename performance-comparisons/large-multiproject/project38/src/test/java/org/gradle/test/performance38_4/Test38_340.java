package org.gradle.test.performance38_4;

import static org.junit.Assert.*;

public class Test38_340 {
    private final Production38_340 production = new Production38_340("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}