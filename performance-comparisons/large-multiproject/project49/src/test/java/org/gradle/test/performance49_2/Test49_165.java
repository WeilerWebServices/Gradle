package org.gradle.test.performance49_2;

import static org.junit.Assert.*;

public class Test49_165 {
    private final Production49_165 production = new Production49_165("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}