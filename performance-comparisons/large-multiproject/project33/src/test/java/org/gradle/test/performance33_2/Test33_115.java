package org.gradle.test.performance33_2;

import static org.junit.Assert.*;

public class Test33_115 {
    private final Production33_115 production = new Production33_115("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}