package org.gradle.hello6;

import org.junit.jupiter.api.Test;

import static org.gradle.hello6.HelloMessageFixture.sampleMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PrintHelloFromJavaTest {

    @Test void testPrint() {
        PrintHelloFromJava classUnderTest = new PrintHelloFromJava();
        assertEquals("""
        Hello Webinar audience from Java 13
            and Gradle 6!""",
            classUnderTest.print(sampleMessage()));
    }
}
