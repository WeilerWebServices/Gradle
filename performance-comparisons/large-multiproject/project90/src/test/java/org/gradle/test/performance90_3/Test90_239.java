package org.gradle.test.performance90_3;

import static org.junit.Assert.*;

public class Test90_239 {
    private final Production90_239 production = new Production90_239("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}