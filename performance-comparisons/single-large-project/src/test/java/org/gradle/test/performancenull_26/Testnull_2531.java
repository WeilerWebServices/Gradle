package org.gradle.test.performancenull_26;

import static org.junit.Assert.*;

public class Testnull_2531 {
    private final Productionnull_2531 production = new Productionnull_2531("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}