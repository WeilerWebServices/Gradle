package org.gradle.test.performancenull_206;

import static org.junit.Assert.*;

public class Testnull_20579 {
    private final Productionnull_20579 production = new Productionnull_20579("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}