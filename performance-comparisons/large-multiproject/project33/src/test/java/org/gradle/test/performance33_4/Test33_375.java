package org.gradle.test.performance33_4;

import static org.junit.Assert.*;

public class Test33_375 {
    private final Production33_375 production = new Production33_375("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}