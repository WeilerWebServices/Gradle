package org.gradle.test.performancenull_450;

import static org.junit.Assert.*;

public class Testnull_44978 {
    private final Productionnull_44978 production = new Productionnull_44978("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}