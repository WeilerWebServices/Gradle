package org.gradle.test.performance35_5;

import static org.junit.Assert.*;

public class Test35_492 {
    private final Production35_492 production = new Production35_492("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}