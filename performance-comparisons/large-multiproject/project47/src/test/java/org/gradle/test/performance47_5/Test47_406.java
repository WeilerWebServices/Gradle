package org.gradle.test.performance47_5;

import static org.junit.Assert.*;

public class Test47_406 {
    private final Production47_406 production = new Production47_406("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}