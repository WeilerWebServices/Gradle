package org.gradle.test.performance90_2;

import static org.junit.Assert.*;

public class Test90_109 {
    private final Production90_109 production = new Production90_109("value");

    @org.junit.Test
    public void test() {
        assertEquals(production.getProperty(), "value");
    }
}