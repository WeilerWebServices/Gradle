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

package org.eclipse.jetty.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * URL Encoding / Decoding Tests
 */
public class URLEncodedTest
{
    @TestFactory
    public Iterator<DynamicTest> testUrlEncoded()
    {
        List<DynamicTest> tests = new ArrayList<>();

        tests.add(dynamicTest("Initially not empty", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            assertEquals(0, url_encoded.size());
        }));

        tests.add(dynamicTest("Not empty after decode(\"\")", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("");
            assertEquals(0, url_encoded.size());
        }));

        tests.add(dynamicTest("Simple encode", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name1=Value1");
            assertEquals(1, url_encoded.size(), "simple param size");
            assertEquals("Name1=Value1", url_encoded.encode(), "simple encode");
            assertEquals("Value1", url_encoded.getString("Name1"), "simple get");
        }));

        tests.add(dynamicTest("Dangling param", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name2=");
            assertEquals(1, url_encoded.size(), "dangling param size");
            assertEquals("Name2", url_encoded.encode(), "dangling encode");
            assertEquals("", url_encoded.getString("Name2"), "dangling get");
        }));

        tests.add(dynamicTest("noValue param", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name3");
            assertEquals(1, url_encoded.size(), "noValue param size");
            assertEquals("Name3", url_encoded.encode(), "noValue encode");
            assertEquals("", url_encoded.getString("Name3"), "noValue get");
        }));

        tests.add(dynamicTest("badly encoded param", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name4=V\u0629lue+4%21");
            assertEquals(1, url_encoded.size(), "encoded param size");
            assertEquals("Name4=V%D8%A9lue+4%21", url_encoded.encode(), "encoded encode");
            assertEquals("V\u0629lue 4!", url_encoded.getString("Name4"), "encoded get");
        }));

        tests.add(dynamicTest("encoded param 1", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name4=Value%2B4%21");
            assertEquals(1, url_encoded.size(), "encoded param size");
            assertEquals("Name4=Value%2B4%21", url_encoded.encode(), "encoded encode");
            assertEquals("Value+4!", url_encoded.getString("Name4"), "encoded get");
        }));

        tests.add(dynamicTest("encoded param 2", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name4=Value+4%21%20%214");
            assertEquals(1, url_encoded.size(), "encoded param size");
            assertEquals("Name4=Value+4%21+%214", url_encoded.encode(), "encoded encode");
            assertEquals("Value 4! !4", url_encoded.getString("Name4"), "encoded get");
        }));

        tests.add(dynamicTest("multi param", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name5=aaa&Name6=bbb");
            assertEquals(2, url_encoded.size(), "multi param size");
            assertTrue(url_encoded.encode().equals("Name5=aaa&Name6=bbb") ||
                            url_encoded.encode().equals("Name6=bbb&Name5=aaa"),
                    "multi encode " + url_encoded.encode());
            assertEquals("aaa", url_encoded.getString("Name5"), "multi get");
            assertEquals("bbb", url_encoded.getString("Name6"), "multi get");
        }));

        tests.add(dynamicTest("multiple value encoded", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name7=aaa&Name7=b%2Cb&Name7=ccc");
            assertEquals("Name7=aaa&Name7=b%2Cb&Name7=ccc", url_encoded.encode(), "multi encode");
            assertEquals("aaa,b,b,ccc", url_encoded.getString("Name7"), "list get all");
            assertEquals("aaa", url_encoded.getValues("Name7").get(0), "list get");
            assertEquals("b,b", url_encoded.getValues("Name7").get(1), "list get");
            assertEquals("ccc", url_encoded.getValues("Name7").get(2), "list get");
        }));

        tests.add(dynamicTest("encoded param", () -> {
            UrlEncoded url_encoded = new UrlEncoded();
            url_encoded.clear();
            url_encoded.decode("Name8=xx%2C++yy++%2Czz");
            assertEquals(1, url_encoded.size(), "encoded param size");
            assertEquals("Name8=xx%2C++yy++%2Czz", url_encoded.encode(), "encoded encode");
            assertEquals("xx,  yy  ,zz", url_encoded.getString("Name8"), "encoded get");
        }));

        return tests.iterator();
    }

    @TestFactory
    public Iterator<DynamicTest> testUrlEncodedStream()
    {
        String[][] charsets = new String[][]
                {
                        {StringUtil.__UTF8, null, "%30"},
                        {StringUtil.__ISO_8859_1, StringUtil.__ISO_8859_1, "%30"},
                        {StringUtil.__UTF8, StringUtil.__UTF8, "%30"},
                        {StringUtil.__UTF16, StringUtil.__UTF16, "%00%30"},
                };

        // Note: "%30" -> decode -> "0"

        List<DynamicTest> tests = new ArrayList<>();

        for(String[] params: charsets)
        {
            tests.add(dynamicTest(params[0] + ">" + params[1] + "|" + params[2],
                    () -> {
                        try (ByteArrayInputStream in = new ByteArrayInputStream(("name\n=value+" + params[2] + "&name1=&name2&n\u00e3me3=value+3").getBytes(params[0])))
                        {
                            MultiMap<String> m = new MultiMap<>();
                            UrlEncoded.decodeTo(in, m, params[1] == null ? null : Charset.forName(params[1]), -1, -1);
                            assertEquals(4, m.size(), params[1] + " stream length");
                            assertThat(params[1] + " stream name\\n", m.getString("name\n"), is("value 0"));
                            assertThat(params[1] + " stream name1", m.getString("name1"), is(""));
                            assertThat(params[1] + " stream name2", m.getString("name2"), is(""));
                            assertThat(params[1] + " stream n\u00e3me3", m.getString("n\u00e3me3"), is("value 3"));
                        }
                    }
            ));
        }


        if (java.nio.charset.Charset.isSupported("Shift_JIS"))
        {
            tests.add(dynamicTest("Shift_JIS",
                    () -> {
                        try(ByteArrayInputStream in2 = new ByteArrayInputStream("name=%83e%83X%83g".getBytes(StandardCharsets.ISO_8859_1)))
                        {
                            MultiMap<String> m2 = new MultiMap<>();
                            UrlEncoded.decodeTo(in2, m2, Charset.forName("Shift_JIS"), -1, -1);
                            assertEquals(1, m2.size(), "stream length");
                            assertEquals("\u30c6\u30b9\u30c8", m2.getString("name"), "stream name");
                        }
                    }
            ));
        }

        return tests.iterator();
    }


    /* -------------------------------------------------------------- */
    @Test
    @EnabledIfSystemProperty(named = "org.eclipse.jetty.util.UrlEncoding.charset", matches = "\\p{Alnum}")
    public void testCharsetViaSystemProperty()
            throws Exception
    {
        try (ByteArrayInputStream in3 = new ByteArrayInputStream("name=libell%E9".getBytes(StringUtil.__ISO_8859_1)))
        {
            MultiMap m3 = new MultiMap();
            Charset nullCharset = null; // use the one from the system property
            UrlEncoded.decodeTo(in3, m3, nullCharset, -1, -1);
            assertEquals("libell\u00E9", m3.getString("name"), "stream name");
        }
    }

    /* -------------------------------------------------------------- */
    @Test
    public void testUtf8()
            throws Exception
    {
        UrlEncoded url_encoded = new UrlEncoded();
        assertEquals(0, url_encoded.size(), "Empty");

        url_encoded.clear();
        url_encoded.decode("text=%E0%B8%9F%E0%B8%AB%E0%B8%81%E0%B8%A7%E0%B8%94%E0%B8%B2%E0%B9%88%E0%B8%81%E0%B8%9F%E0%B8%A7%E0%B8%AB%E0%B8%AA%E0%B8%94%E0%B8%B2%E0%B9%88%E0%B8%AB%E0%B8%9F%E0%B8%81%E0%B8%A7%E0%B8%94%E0%B8%AA%E0%B8%B2%E0%B8%9F%E0%B8%81%E0%B8%AB%E0%B8%A3%E0%B8%94%E0%B9%89%E0%B8%9F%E0%B8%AB%E0%B8%99%E0%B8%81%E0%B8%A3%E0%B8%94%E0%B8%B5&Action=Submit");

        String hex = "E0B89FE0B8ABE0B881E0B8A7E0B894E0B8B2E0B988E0B881E0B89FE0B8A7E0B8ABE0B8AAE0B894E0B8B2E0B988E0B8ABE0B89FE0B881E0B8A7E0B894E0B8AAE0B8B2E0B89FE0B881E0B8ABE0B8A3E0B894E0B989E0B89FE0B8ABE0B899E0B881E0B8A3E0B894E0B8B5";
        String expected = new String(TypeUtil.fromHexString(hex), "utf-8");
        assertEquals(expected, url_encoded.getString("text"));
    }

    @Test
    public void testUtf8_MultiByteCodePoint()
    {
        String input = "text=test%C3%A4";
        UrlEncoded url_encoded = new UrlEncoded();
        url_encoded.decode(input);

        // http://www.ltg.ed.ac.uk/~richard/utf-8.cgi?input=00e4&mode=hex
        // Should be "testä"
        // "test" followed by a LATIN SMALL LETTER A WITH DIAERESIS

        String expected = "test\u00e4";
        assertThat(url_encoded.getString("text"), is(expected));
    }
}
