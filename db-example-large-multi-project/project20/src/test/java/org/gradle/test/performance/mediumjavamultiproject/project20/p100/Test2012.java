package org.gradle.test.performance.mediumjavamultiproject.project20.p100;

import org.gradle.test.performance.mediumjavamultiproject.project17.p85.Production1712;
import org.gradle.test.performance.mediumjavamultiproject.project18.p90.Production1812;
import org.gradle.test.performance.mediumjavamultiproject.project19.p95.Production1912;

import org.junit.Test;
import static org.junit.Assert.*;

public class Test2012 {  
    Production2012 objectUnderTest = new Production2012();     

    @Test
    public void testProperty0() throws Exception {
        Production2003 value = new Production2003();
        objectUnderTest.setProperty0(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty0());
    }

    @Test
    public void testProperty1() throws Exception {
        Production2007 value = new Production2007();
        objectUnderTest.setProperty1(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty1());
    }

    @Test
    public void testProperty2() throws Exception {
        Production2011 value = new Production2011();
        objectUnderTest.setProperty2(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty2());
    }

    @Test
    public void testProperty3() throws Exception {
        Production1712 value = new Production1712();
        objectUnderTest.setProperty3(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty3());
    }

    @Test
    public void testProperty4() throws Exception {
        Production1812 value = new Production1812();
        objectUnderTest.setProperty4(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty4());
    }

    @Test
    public void testProperty5() throws Exception {
        Production1912 value = new Production1912();
        objectUnderTest.setProperty5(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty5());
    }

    @Test
    public void testProperty6() throws Exception {
        String value = "value";
        objectUnderTest.setProperty6(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty6());
    }

    @Test
    public void testProperty7() throws Exception {
        String value = "value";
        objectUnderTest.setProperty7(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty7());
    }

    @Test
    public void testProperty8() throws Exception {
        String value = "value";
        objectUnderTest.setProperty8(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty8());
    }

    @Test
    public void testProperty9() throws Exception {
        String value = "value";
        objectUnderTest.setProperty9(value);
        Thread.sleep(250);
        assertEquals(value, objectUnderTest.getProperty9());
    }

}