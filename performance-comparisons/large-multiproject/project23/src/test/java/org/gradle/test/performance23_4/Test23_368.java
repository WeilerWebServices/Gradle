package org.gradle.test.performance23_4;

import static org.junit.Assert.*;

public class Test23_368 {
    private final Production23_368 production = new Production23_368("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}