package org.gradle.test.performance98_5;

import static org.junit.Assert.*;

public class Test98_404 {
    private final Production98_404 production = new Production98_404("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}