package org.gradle.test.performance24_3;

import static org.junit.Assert.*;

public class Test24_294 {
    private final Production24_294 production = new Production24_294("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}