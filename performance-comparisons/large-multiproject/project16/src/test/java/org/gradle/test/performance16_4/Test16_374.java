package org.gradle.test.performance16_4;

import static org.junit.Assert.*;

public class Test16_374 {
    private final Production16_374 production = new Production16_374("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}