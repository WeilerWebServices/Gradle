package org.gradle.test.performance86_2;

import static org.junit.Assert.*;

public class Test86_112 {
    private final Production86_112 production = new Production86_112("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}