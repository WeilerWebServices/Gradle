package org.gradle.test.performance11_2;

import static org.junit.Assert.*;

public class Test11_148 {
    private final Production11_148 production = new Production11_148("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}