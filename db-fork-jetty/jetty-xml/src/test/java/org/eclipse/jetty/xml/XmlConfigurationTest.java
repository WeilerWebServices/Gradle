//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class XmlConfigurationTest
{
    protected String[] _configure=new String [] {"org/eclipse/jetty/xml/configureWithAttr.xml","org/eclipse/jetty/xml/configureWithElements.xml"};

    private static final String STRING_ARRAY_XML = "<Array type=\"String\"><Item type=\"String\">String1</Item><Item type=\"String\">String2</Item></Array>";
    private static final String INT_ARRAY_XML = "<Array type=\"int\"><Item type=\"int\">1</Item><Item type=\"int\">2</Item></Array>";

    @Test
    public void testMortBay() throws Exception
    {
        URL url = XmlConfigurationTest.class.getClassLoader().getResource("org/eclipse/jetty/xml/mortbay.xml");
        XmlConfiguration configuration = new XmlConfiguration(url);
        configuration.configure();
    }

    @Test
    public void testPassedObject() throws Exception
    {
        for (String configure : _configure)
        {
            Map<String,String> properties = new HashMap<>();
            properties.put("whatever", "xxx");
            TestConfiguration.VALUE=77;
            URL url = XmlConfigurationTest.class.getClassLoader().getResource(configure);
            XmlConfiguration configuration = new XmlConfiguration(url);
            TestConfiguration tc = new TestConfiguration("tc");
            configuration.getProperties().putAll(properties);
            configuration.configure(tc);

            assertEquals("SetValue", tc.testObject, "Set String");
            assertEquals(2, tc.testInt, "Set Type");

            assertEquals(18080, tc.propValue);

            assertEquals("PutValue", tc.get("Test"), "Put");
            assertEquals("2", tc.get("TestDft"), "Put dft");
            assertEquals(2, tc.get("TestInt"), "Put type");

            assertEquals("PutValue", tc.get("Trim"), "Trim");
            assertEquals(null, tc.get("Null"), "Null");
            assertEquals(null, tc.get("NullTrim"), "NullTrim");

            assertEquals(1.2345, tc.get("ObjectTrim"), "ObjectTrim");
            assertEquals("-1String", tc.get("Objects"), "Objects");
            assertEquals("-1String", tc.get("ObjectsTrim"), "ObjectsTrim");
            assertEquals("\n    PutValue\n  ", tc.get("String"), "String");
            assertEquals("", tc.get("NullString"), "NullString");
            assertEquals("\n  ", tc.get("WhiteSpace"), "WhiteSpace");
            assertEquals("\n    1.2345\n  ", tc.get("ObjectString"), "ObjectString");
            assertEquals("-1String", tc.get("ObjectsString"), "ObjectsString");
            assertEquals("-1\n  String", tc.get("ObjectsWhiteString"), "ObjectsWhiteString");

            assertEquals(System.getProperty("user.dir")+"/stuff", tc.get("SystemProperty"), "SystemProperty");
            assertEquals(System.getenv("HOME"), tc.get("Env"), "Env");

            assertEquals("xxx", tc.get("Property"), "Property");


            assertEquals("Yes", tc.get("Called"), "Called");

            assertTrue(TestConfiguration.called);

            assertEquals("Blah", tc.oa[0], "oa[0]");
            assertEquals("1.2.3.4:5678", tc.oa[1], "oa[1]");
            assertEquals(1.2345, tc.oa[2], "oa[2]");
            assertEquals(null, tc.oa[3], "oa[3]");

            assertEquals(1, tc.ia[0], "ia[0]");
            assertEquals(2, tc.ia[1], "ia[1]");
            assertEquals(3, tc.ia[2], "ia[2]");
            assertEquals(0, tc.ia[3], "ia[3]");

            TestConfiguration tc2=tc.nested;
            assertTrue(tc2!=null);
            assertEquals(true, tc2.get("Arg"), "Called(bool)");

            assertEquals(null, tc.get("Arg"), "nested config");
            assertEquals(true, tc2.get("Arg"), "nested config");

            assertEquals("Call1", tc2.testObject, "nested config");
            assertEquals(4, tc2.testInt, "nested config");
            assertEquals("http://www.eclipse.com/", tc2.url.toString(), "nested call");

            assertEquals(tc.testField1, 77, "static to field");
            assertEquals(tc.testField2, 2, "field to field");
            assertEquals(TestConfiguration.VALUE, 42, "literal to static");
            
            assertEquals(((Map<String, String>)configuration.getIdMap().get("map")).get("key0"), "value0");
            assertEquals(((Map<String, String>)configuration.getIdMap().get("map")).get("key1"), "value1");
        }
    }

    @Test
    public void testNewObject() throws Exception
    {
        for (String configure : _configure)
        {
            TestConfiguration.VALUE=71;
            Map<String,String> properties = new HashMap<>();
            properties.put("whatever", "xxx");
            
            URL url = XmlConfigurationTest.class.getClassLoader().getResource(configure);
            final AtomicInteger count = new AtomicInteger(0);
            XmlConfiguration configuration = new XmlConfiguration(url)
            {
                @Override
                public void initializeDefaults(Object object)
                {
                    if (object instanceof TestConfiguration)
                    {
                        count.incrementAndGet();
                        ((TestConfiguration)object).setNested(null);
                        ((TestConfiguration)object).setTestString("NEW DEFAULT");
                    }
                }
            };
            configuration.getProperties().putAll(properties);
            TestConfiguration tc = (TestConfiguration)configuration.configure();

            assertEquals(3,count.get());

            assertEquals("NEW DEFAULT",tc.getTestString());
            assertEquals("nested",tc.getNested().getTestString());
            assertEquals("NEW DEFAULT",tc.getNested().getNested().getTestString());

            assertEquals("SetValue", tc.testObject, "Set String");
            assertEquals(2, tc.testInt, "Set Type");

            assertEquals(18080, tc.propValue);

            assertEquals("PutValue", tc.get("Test"), "Put");
            assertEquals("2", tc.get("TestDft"), "Put dft");
            assertEquals(2, tc.get("TestInt"), "Put type");

            assertEquals("PutValue", tc.get("Trim"), "Trim");
            assertEquals(null, tc.get("Null"), "Null");
            assertEquals(null, tc.get("NullTrim"), "NullTrim");

            assertEquals(1.2345, tc.get("ObjectTrim"), "ObjectTrim");
            assertEquals("-1String", tc.get("Objects"), "Objects");
            assertEquals("-1String", tc.get("ObjectsTrim"), "ObjectsTrim");
            assertEquals("\n    PutValue\n  ", tc.get("String"), "String");
            assertEquals("", tc.get("NullString"), "NullString");
            assertEquals("\n  ", tc.get("WhiteSpace"), "WhiteSpace");
            assertEquals("\n    1.2345\n  ", tc.get("ObjectString"), "ObjectString");
            assertEquals("-1String", tc.get("ObjectsString"), "ObjectsString");
            assertEquals("-1\n  String", tc.get("ObjectsWhiteString"), "ObjectsWhiteString");

            assertEquals(System.getProperty("user.dir")+"/stuff", tc.get("SystemProperty"), "SystemProperty");
            assertEquals("xxx", tc.get("Property"), "Property");


            assertEquals("Yes", tc.get("Called"), "Called");

            assertTrue(TestConfiguration.called);

            assertEquals("Blah", tc.oa[0], "oa[0]");
            assertEquals("1.2.3.4:5678", tc.oa[1], "oa[1]");
            assertEquals(1.2345, tc.oa[2], "oa[2]");
            assertEquals(null, tc.oa[3], "oa[3]");

            assertEquals(1, tc.ia[0], "ia[0]");
            assertEquals(2, tc.ia[1], "ia[1]");
            assertEquals(3, tc.ia[2], "ia[2]");
            assertEquals(0, tc.ia[3], "ia[3]");

            TestConfiguration tc2=tc.nested;
            assertTrue(tc2!=null);
            assertEquals(true, tc2.get("Arg"), "Called(bool)");

            assertEquals(null, tc.get("Arg"), "nested config");
            assertEquals(true, tc2.get("Arg"), "nested config");

            assertEquals("Call1", tc2.testObject, "nested config");
            assertEquals(4, tc2.testInt, "nested config");
            assertEquals("http://www.eclipse.com/", tc2.url.toString(), "nested call");

            assertEquals(71, tc.testField1, "static to field");
            assertEquals(2, tc.testField2, "field to field");
            assertEquals(42, TestConfiguration.VALUE, "literal to static");
        }
    }


    @Test
    public void testGetClass() throws Exception
    {
        XmlConfiguration configuration =
            new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"Test\"><Get name=\"class\"/></Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        configuration.configure(tc);
        assertEquals(TestConfiguration.class,tc.testObject);
        
        configuration =
            new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"Test\"><Get class=\"java.lang.String\" name=\"class\"><Get id=\"simple\" name=\"simpleName\"/></Get></Set></Configure>");
        configuration.configure(tc);
        assertEquals(String.class,tc.testObject);
        assertEquals("String",configuration.getIdMap().get("simple"));
    }
    
    @Test
    public void testStringConfiguration() throws Exception
    {
        XmlConfiguration configuration =
            new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"Test\">SetValue</Set><Set name=\"Test\" type=\"int\">2</Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        configuration.configure(tc);
        assertEquals("SetValue", tc.testObject, "Set String 3");
        assertEquals(2, tc.testInt, "Set Type 3");
    }

    @Test
    public void testMeaningfullSetException() throws Exception
    {
        XmlConfiguration configuration =
            new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"PropertyTest\"><Property name=\"null\"/></Set></Configure>");
        TestConfiguration tc = new TestConfiguration();

        NoSuchMethodException e = assertThrows(NoSuchMethodException.class, () -> {
            configuration.configure(tc);
        });

        assertThat(e.getMessage(), containsString("Found setters for int"));
    }

    @Test
    public void testListConstructorArg() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">"
                + "<Set name=\"constructorArgTestClass\"><New class=\"org.eclipse.jetty.xml.ConstructorArgTestClass\"><Arg type=\"List\">"
                + STRING_ARRAY_XML + "</Arg></New></Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getList() returns null as it's not configured yet",tc.getList(),is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getList() returns not null",tc.getList(),not(nullValue()));
        assertThat("tc.getList() has two entries as specified in the xml", tc.getList().size(), is(2));
    }

    @Test
    public void testTwoArgumentListConstructorArg() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">"
                + "<Set name=\"constructorArgTestClass\"><New class=\"org.eclipse.jetty.xml.ConstructorArgTestClass\">"
                + "<Arg type=\"List\">" + STRING_ARRAY_XML + "</Arg>"
                + "<Arg type=\"List\">" + STRING_ARRAY_XML + "</Arg>"
                + "</New></Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getList() returns null as it's not configured yet",tc.getList(),is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getList() returns not null",tc.getList(),not(nullValue()));
        assertThat("tc.getList() has two entries as specified in the xml", tc.getList().size(), is(2));
    }

    @Test
    public void testListNotContainingArray() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">"
                + "<New class=\"org.eclipse.jetty.xml.ConstructorArgTestClass\"><Arg type=\"List\">Some String</Arg></New></Configure>");
        TestConfiguration tc = new TestConfiguration();

        assertThrows(IllegalArgumentException.class, ()-> {
            xmlConfiguration.configure(tc);
        });
    }

    @Test
    public void testSetConstructorArg() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">"
                + "<Set name=\"constructorArgTestClass\"><New class=\"org.eclipse.jetty.xml.ConstructorArgTestClass\"><Arg type=\"Set\">"
                + STRING_ARRAY_XML + "</Arg></New></Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getList() returns null as it's not configured yet",tc.getSet(),is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getList() returns not null",tc.getSet(),not(nullValue()));
        assertThat("tc.getList() has two entries as specified in the xml", tc.getSet().size(), is(2));
    }

    @Test
    public void testSetNotContainingArray() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">"
                + "<New class=\"org.eclipse.jetty.xml.ConstructorArgTestClass\"><Arg type=\"Set\">Some String</Arg></New></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThrows(IllegalArgumentException.class, ()->{
            xmlConfiguration.configure(tc);
        });
    }

    @Test
    public void testListSetterWithStringArray() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"List\">"
                + STRING_ARRAY_XML + "</Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getList() returns null as it's not configured yet",tc.getList(),is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getList() has two entries as specified in the xml", tc.getList().size(), is(2));
    }

    @Test
    public void testListSetterWithPrimitiveArray() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"List\">"
                + INT_ARRAY_XML + "</Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getList() returns null as it's not configured yet",tc.getList(),is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getList() has two entries as specified in the xml", tc.getList().size(), is(2));
    }

    @Test
    public void testNotSupportedLinkedListSetter() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"LinkedList\">"
                + INT_ARRAY_XML + "</Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getSet() returns null as it's not configured yet", tc.getList(), is(nullValue()));
        assertThrows(NoSuchMethodException.class, ()->{
            xmlConfiguration.configure(tc);
        });
    }

    @Test
    public void testArrayListSetter() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"ArrayList\">"
                + INT_ARRAY_XML + "</Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getSet() returns null as it's not configured yet", tc.getList(), is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getSet() has two entries as specified in the xml", tc.getList().size(), is(2));
    }

    @Test
    public void testSetSetter() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"Set\">"
                + STRING_ARRAY_XML + "</Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getSet() returns null as it's not configured yet", tc.getSet(), is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getSet() has two entries as specified in the xml", tc.getSet().size(), is(2));
    }

    @Test
    public void testSetSetterWithPrimitiveArray() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\"><Set name=\"Set\">"
                + INT_ARRAY_XML + "</Set></Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertThat("tc.getSet() returns null as it's not configured yet", tc.getSet(), is(nullValue()));
        xmlConfiguration.configure(tc);
        assertThat("tc.getSet() has two entries as specified in the xml", tc.getSet().size(), is(2));
    }

    @Test
    public void testMap() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">" +
                "    <Set name=\"map\">" +
                "        <Map>" +
                "            <Entry>" +
                "                <Item>key1</Item>" +
                "                <Item>value1</Item>" +
                "            </Entry>" +
                "            <Entry>" +
                "                <Item>key2</Item>" +
                "                <Item>value2</Item>" +
                "            </Entry>" +
                "        </Map>" +
                "    </Set>" +
                "</Configure>");
        TestConfiguration tc = new TestConfiguration();
        assertNull(tc.map, "tc.map is null as it's not configured yet");
        xmlConfiguration.configure(tc);
        assertEquals(2, tc.map.size(), "tc.map is has two entries as specified in the XML");
    }

    @Test
    public void testConstructorNamedInjection() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg>arg1</Arg>  " +
                "  <Arg>arg2</Arg>  " +
                "  <Arg>arg3</Arg>  " +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
    }

    @Test
    public void testConstructorNamedInjectionOrdered() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "  <Arg name=\"second\">arg2</Arg>  " +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
    }

    @Test
    public void testConstructorNamedInjectionUnOrdered() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "  <Arg name=\"second\">arg2</Arg>  " +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
    }

    @Test
    public void testConstructorNamedInjectionOrderedMixed() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "  <Arg>arg2</Arg>  " +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
    }

    @Test
    public void testConstructorNamedInjectionUnorderedMixed() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "  <Arg>arg2</Arg>  " +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
    }

    @Test
    public void testNestedConstructorNamedInjection() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg>arg1</Arg>  " +
                "  <Arg>arg2</Arg>  " +
                "  <Arg>arg3</Arg>  " +
                "  <Set name=\"nested\">  " +
                "    <New class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "      <Arg>arg1</Arg>  " +
                "      <Arg>arg2</Arg>  " +
                "      <Arg>arg3</Arg>  " +
                "    </New>" +
                "  </Set>" +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
        assertEquals("arg1", atc.getNested().getFirst(), "nested first parameter not wired correctly");
        assertEquals("arg2", atc.getNested().getSecond(), "nested second parameter not wired correctly");
        assertEquals("arg3", atc.getNested().getThird(), "nested third parameter not wired correctly");

    }

    @Test
    public void testNestedConstructorNamedInjectionOrdered() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "  <Arg name=\"second\">arg2</Arg>  " +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "  <Set name=\"nested\">  " +
                "    <New class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "      <Arg name=\"first\">arg1</Arg>  " +
                "      <Arg name=\"second\">arg2</Arg>  " +
                "      <Arg name=\"third\">arg3</Arg>  " +
                "    </New>" +
                "  </Set>" +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
        assertEquals("arg1", atc.getNested().getFirst(), "nested first parameter not wired correctly");
        assertEquals("arg2", atc.getNested().getSecond(), "nested second parameter not wired correctly");
        assertEquals("arg3", atc.getNested().getThird(), "nested third parameter not wired correctly");
    }

    @Test
    public void testNestedConstructorNamedInjectionUnOrdered() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "  <Arg name=\"second\">arg2</Arg>  " +
                "  <Set name=\"nested\">  " +
                "    <New class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "      <Arg name=\"first\">arg1</Arg>  " +
                "      <Arg name=\"third\">arg3</Arg>  " +
                "      <Arg name=\"second\">arg2</Arg>  " +
                "    </New>" +
                "  </Set>" +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
        assertEquals("arg1", atc.getNested().getFirst(), "nested first parameter not wired correctly");
        assertEquals("arg2", atc.getNested().getSecond(), "nested second parameter not wired correctly");
        assertEquals("arg3", atc.getNested().getThird(), "nested third parameter not wired correctly");
    }

    @Test
    public void testNestedConstructorNamedInjectionOrderedMixed() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "  <Arg>arg2</Arg>  " +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "  <Set name=\"nested\">  " +
                "    <New class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "      <Arg name=\"first\">arg1</Arg>  " +
                "      <Arg>arg2</Arg>  " +
                "      <Arg name=\"third\">arg3</Arg>  " +
                "    </New>" +
                "  </Set>" +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
        assertEquals("arg1", atc.getNested().getFirst(), "nested first parameter not wired correctly");
        assertEquals("arg2", atc.getNested().getSecond(), "nested second parameter not wired correctly");
        assertEquals("arg3", atc.getNested().getThird(), "nested third parameter not wired correctly");
    }
    
    @Test
    public void testArgumentsGetIgnoredMissingDTD() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration(new ByteArrayInputStream(("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg>arg1</Arg>  " +
                "  <Arg>arg2</Arg>  " +
                "  <Arg>arg3</Arg>  " +
                "  <Set name=\"nested\">  " +
                "    <New class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">\n" + 
                "      <Arg>arg1</Arg>\n" + 
                "      <Arg>arg2</Arg>\n" + 
                "      <Arg>arg3</Arg>\n" + 
                "    </New>" +
                "  </Set>" +
                "</Configure>").getBytes(StandardCharsets.ISO_8859_1)));
//        XmlConfiguration xmlConfiguration = new XmlConfiguration(url);

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
        assertEquals("arg1", atc.getNested().getFirst(), "nested first parameter not wired correctly");
        assertEquals("arg2", atc.getNested().getSecond(), "nested second parameter not wired correctly");
        assertEquals("arg3", atc.getNested().getThird(), "nested third parameter not wired correctly");
    }

    @Test
    public void testSetGetIgnoredMissingDTD() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration(new ByteArrayInputStream(("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\">arg1</Set>  " +
                "  <Set name=\"second\">arg2</Set>  " +
                "  <Set name=\"third\">arg3</Set>  " +
                "  <Set name=\"nested\">  " +
                "    <New class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">\n" + 
                "      <Set name=\"first\">arg1</Set>  " +
                "      <Set name=\"second\">arg2</Set>  " +
                "      <Set name=\"third\">arg3</Set>  " +
                "    </New>" +
                "  </Set>" +
                "</Configure>").getBytes(StandardCharsets.UTF_8)));
//        XmlConfiguration xmlConfiguration = new XmlConfiguration(url);

        DefaultTestConfiguration atc = (DefaultTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
        assertEquals("arg1", atc.getNested().getFirst(), "nested first parameter not wired correctly");
        assertEquals("arg2", atc.getNested().getSecond(), "nested second parameter not wired correctly");
        assertEquals("arg3", atc.getNested().getThird(), "nested third parameter not wired correctly");
    }

    @Test
    public void testNestedConstructorNamedInjectionUnorderedMixed() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "  <Arg name=\"third\">arg3</Arg>  " +
                "  <Arg>arg2</Arg>  " +
                "  <Arg name=\"first\">arg1</Arg>  " +
                "  <Set name=\"nested\">  " +
                "    <New class=\"org.eclipse.jetty.xml.AnnotatedTestConfiguration\">" +
                "      <Arg name=\"third\">arg3</Arg>  " +
                "      <Arg>arg2</Arg>  " +
                "      <Arg name=\"first\">arg1</Arg>  " +
                "    </New>" +
                "  </Set>" +
                "</Configure>");

        AnnotatedTestConfiguration atc = (AnnotatedTestConfiguration)xmlConfiguration.configure();

        assertEquals("arg1", atc.getFirst(), "first parameter not wired correctly");
        assertEquals("arg2", atc.getSecond(), "second parameter not wired correctly");
        assertEquals("arg3", atc.getThird(), "third parameter not wired correctly");
        assertEquals("arg1", atc.getNested().getFirst(), "nested first parameter not wired correctly");
        assertEquals("arg2", atc.getNested().getSecond(), "nested second parameter not wired correctly");
        assertEquals("arg3", atc.getNested().getThird(), "nested third parameter not wired correctly");
    }

    public static class NativeHolder
    {
        private boolean _boolean;
        private int _integer;
        private float _float;

        public boolean getBoolean()
        {
            return _boolean;
        }

        public void setBoolean(boolean value)
        {
            this._boolean = value;
        }

        public int getInteger()
        {
            return _integer;
        }

        public void setInteger(int integer)
        {
            _integer = integer;
        }

        public float getFloat()
        {
            return _float;
        }

        public void setFloat(float f)
        {
            _float = f;
        }
        
    }
    
    @Test
    public void testSetBooleanTrue() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.XmlConfigurationTest$NativeHolder\">" +
                "  <Set name=\"boolean\">true</Set>" +
                "</Configure>");

        NativeHolder bh = (NativeHolder)xmlConfiguration.configure();
        assertTrue(bh.getBoolean());
    }
    
    @Test
    public void testSetBooleanFalse() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.XmlConfigurationTest$NativeHolder\">" +
                "  <Set name=\"boolean\">false</Set>" +
                "</Configure>");

        NativeHolder bh = (NativeHolder)xmlConfiguration.configure();
        assertFalse(bh.getBoolean());
    }
    
    @Test
    @Disabled
    public void testSetBadBoolean() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.XmlConfigurationTest$NativeHolder\">" +
                "  <Set name=\"boolean\">tru</Set>" +
                "</Configure>");

        NativeHolder bh = (NativeHolder)xmlConfiguration.configure();
        assertTrue(bh.getBoolean(), "boolean['tru']");
    }
    
    @Test
    public void testSetBadInteger() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.XmlConfigurationTest$NativeHolder\">" +
                "  <Set name=\"integer\">bad</Set>" +
                "</Configure>");

        assertThrows(InvocationTargetException.class, ()->{
            xmlConfiguration.configure();
        });
    }
    
    @Test
    public void testSetBadExtraInteger() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.XmlConfigurationTest$NativeHolder\">" +
                "  <Set name=\"integer\">100 bas</Set>" +
                "</Configure>");

        assertThrows(InvocationTargetException.class, ()->{
            xmlConfiguration.configure();
        });
    }
    
    @Test
    public void testSetBadFloatInteger() throws Exception
    {
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.XmlConfigurationTest$NativeHolder\">" +
                "  <Set name=\"integer\">1.5</Set>" +
                "</Configure>");

        assertThrows(InvocationTargetException.class, ()->{
            xmlConfiguration.configure();
        });
    }

    @Test
    public void testWithMultiplePropertyNamesWithNoPropertyThenDefaultIsChosen() throws Exception
    {
        // No properties
        String defolt = "baz";
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\"><Property name=\"wibble\" deprecated=\"foo,bar\" default=\"" + defolt + "\"/></Set>  " +
                "</Configure>");
        DefaultTestConfiguration config = (DefaultTestConfiguration)xmlConfiguration.configure();
        assertEquals(defolt, config.getFirst());
    }

    @Test
    public void testWithMultiplePropertyNamesWithFirstPropertyThenFirstIsChosen() throws Exception
    {
        String name = "foo";
        String value = "foo";
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\"><Property name=\"" + name + "\" deprecated=\"other,bar\" default=\"baz\"/></Set>  " +
                "</Configure>");
        xmlConfiguration.getProperties().put(name, value);
        DefaultTestConfiguration config = (DefaultTestConfiguration)xmlConfiguration.configure();
        assertEquals(value, config.getFirst());
    }

    @Test
    public void testWithMultiplePropertyNamesWithSecondPropertyThenSecondIsChosen() throws Exception
    {
        String name = "bar";
        String value = "bar";
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\"><Property name=\"foo\" deprecated=\"" + name + "\" default=\"baz\"/></Set>  " +
                "</Configure>");
        xmlConfiguration.getProperties().put(name, value);
        DefaultTestConfiguration config = (DefaultTestConfiguration)xmlConfiguration.configure();
        assertEquals(value, config.getFirst());
    }

    @Test
    public void testWithMultiplePropertyNamesWithDeprecatedThenThirdIsChosen() throws Exception
    {
        String name = "bar";
        String value = "bar";
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\"><Property name=\"foo\" deprecated=\"other," + name + "\" default=\"baz\"/></Set>  " +
                "</Configure>");
        xmlConfiguration.getProperties().put(name, value);
        DefaultTestConfiguration config = (DefaultTestConfiguration)xmlConfiguration.configure();
        assertEquals(value, config.getFirst());
    }

    @Test
    public void testWithMultiplePropertyNameElementsWithDeprecatedThenThirdIsChosen() throws Exception
    {
        String name = "bar";
        String value = "bar";
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\">" +
                "  <Property>  " +
                "    <Name>foo</Name>" +
                "    <Deprecated>foo</Deprecated>" +
                "    <Deprecated>"+name+"</Deprecated>" +
                "    <Default>baz</Default>" +
                "  </Property>  " +
                "  </Set>  " +
                "</Configure>");
        xmlConfiguration.getProperties().put(name, value);
        DefaultTestConfiguration config = (DefaultTestConfiguration)xmlConfiguration.configure();
        assertEquals(value, config.getFirst());
    }

    @Test
    public void testPropertyNotFoundWithPropertyInDefaultValue() throws Exception
    {
        String name = "bar";
        String value = "bar";
        String defaultValue = "_<Property name=\"bar\"/>_<Property name=\"bar\"/>_";
        String expectedValue = "_" + value + "_" + value + "_";
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\">" +
                "    <Property>" +
                "      <Name>not_found</Name>" +
                "      <Default>" + defaultValue + "</Default>" +
                "    </Property>" +
                "  </Set>  " +
                "</Configure>");
        xmlConfiguration.getProperties().put(name, value);
        DefaultTestConfiguration config = (DefaultTestConfiguration)xmlConfiguration.configure();
        assertEquals(expectedValue, config.getFirst());
    }

    @Test
    public void testPropertyNotFoundWithPropertyInDefaultValueNotFoundWithDefault() throws Exception
    {
        String value = "bar";
        XmlConfiguration xmlConfiguration = new XmlConfiguration("" +
                "<Configure class=\"org.eclipse.jetty.xml.DefaultTestConfiguration\">" +
                "  <Set name=\"first\">" +
                "    <Property name=\"not_found\">" +
                "      <Default><Property name=\"also_not_found\" default=\"" + value + "\"/></Default>" +
                "    </Property>" +
                "  </Set>  " +
                "</Configure>");
        DefaultTestConfiguration config = (DefaultTestConfiguration)xmlConfiguration.configure();
        assertEquals(value, config.getFirst());
    }

    @Test
    public void testJettyStandardIdsAndProperties_JettyHome_JettyBase() throws Exception
    {
        String propNames[] = new String[] {
                "jetty.base",
                "jetty.home"
        };

        for(String propName: propNames)
        {
            XmlConfiguration configuration =
                    new XmlConfiguration("" +
                            "<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">" +
                            "  <Set name=\"TestString\">" +
                            "    <Property name=\"" + propName + "\"/>" +
                            "  </Set>" +
                            "</Configure>");

            configuration.setJettyStandardIdsAndProperties(null, null);

            TestConfiguration tc = new TestConfiguration();
            configuration.configure(tc);

            assertThat(propName, tc.getTestString(), is(notNullValue()));
            assertThat(propName, tc.getTestString(), not(startsWith("file:")));
        }
    }

    @Test
    public void testJettyStandardIdsAndProperties_JettyHomeUri_JettyBaseUri() throws Exception
    {
        String propNames[] = new String[] {
                "jetty.base.uri",
                "jetty.home.uri"
        };

        for(String propName: propNames)
        {
            XmlConfiguration configuration =
                    new XmlConfiguration("" +
                            "<Configure class=\"org.eclipse.jetty.xml.TestConfiguration\">" +
                            "  <Set name=\"TestString\">" +
                            "    <Property name=\"" + propName + "\"/>" +
                            "  </Set>" +
                            "</Configure>");

            configuration.setJettyStandardIdsAndProperties(null, null);

            TestConfiguration tc = new TestConfiguration();
            configuration.configure(tc);

            assertThat(propName, tc.getTestString(), is(notNullValue()));
            assertThat(propName, tc.getTestString(), startsWith("file:"));
        }
    }
}
