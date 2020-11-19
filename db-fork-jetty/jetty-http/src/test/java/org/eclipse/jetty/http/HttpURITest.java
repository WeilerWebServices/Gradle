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


package org.eclipse.jetty.http;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.util.MultiMap;
import org.junit.jupiter.api.Test;

public class HttpURITest
{
    @Test
    public void testInvalidAddress() throws Exception
    {
        assertInvalidURI("http://[ffff::1:8080/", "Invalid URL; no closing ']' -- should throw exception");
        assertInvalidURI("**", "only '*', not '**'");
        assertInvalidURI("*/", "only '*', not '*/'");
    }

    private void assertInvalidURI(String invalidURI, String message)
    {
        HttpURI uri = new HttpURI();
        try
        {
            uri.parse(invalidURI);
            fail(message);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    @Test
    public void testParse()
    {
        HttpURI uri = new HttpURI();

        uri.parse("*");
        assertThat(uri.getHost(),nullValue());
        assertThat(uri.getPath(),is("*"));
        
        uri.parse("/foo/bar");
        assertThat(uri.getHost(),nullValue());
        assertThat(uri.getPath(),is("/foo/bar"));
        
        uri.parse("//foo/bar");
        assertThat(uri.getHost(),is("foo"));
        assertThat(uri.getPath(),is("/bar"));
        
        uri.parse("http://foo/bar");
        assertThat(uri.getHost(),is("foo"));
        assertThat(uri.getPath(),is("/bar"));
    }

    @Test
    public void testParseRequestTarget()
    {
        HttpURI uri = new HttpURI();

        uri.parseRequestTarget("GET","*");
        assertThat(uri.getHost(),nullValue());
        assertThat(uri.getPath(),is("*"));
        
        uri.parseRequestTarget("GET","/foo/bar");
        assertThat(uri.getHost(),nullValue());
        assertThat(uri.getPath(),is("/foo/bar"));
        
        uri.parseRequestTarget("GET","//foo/bar");
        assertThat(uri.getHost(),nullValue());
        assertThat(uri.getPath(),is("//foo/bar"));
        
        uri.parseRequestTarget("GET","http://foo/bar");
        assertThat(uri.getHost(),is("foo"));
        assertThat(uri.getPath(),is("/bar"));
    }

    @Test
    public void testExtB() throws Exception
    {
        for (String value: new String[]{"a","abcdABCD","\u00C0","\u697C","\uD869\uDED5","\uD840\uDC08"} )
        {
            HttpURI uri = new HttpURI("/path?value="+URLEncoder.encode(value,"UTF-8"));

            MultiMap<String> parameters = new MultiMap<>();
            uri.decodeQueryTo(parameters,StandardCharsets.UTF_8);
            assertEquals(value,parameters.getString("value"));
        }
    }

    @Test
    public void testAt() throws Exception
    {
        HttpURI uri = new HttpURI("/@foo/bar");
        assertEquals("/@foo/bar",uri.getPath());
    }

    @Test
    public void testParams() throws Exception
    {
        HttpURI uri = new HttpURI("/foo/bar");
        assertEquals("/foo/bar",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());
        assertEquals(null,uri.getParam());
        
        uri = new HttpURI("/foo/bar;jsessionid=12345");
        assertEquals("/foo/bar;jsessionid=12345",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());
        assertEquals("jsessionid=12345",uri.getParam());
        
        uri = new HttpURI("/foo;abc=123/bar;jsessionid=12345");
        assertEquals("/foo;abc=123/bar;jsessionid=12345",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());
        assertEquals("jsessionid=12345",uri.getParam());
        
        uri = new HttpURI("/foo;abc=123/bar;jsessionid=12345?name=value");
        assertEquals("/foo;abc=123/bar;jsessionid=12345",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());
        assertEquals("jsessionid=12345",uri.getParam());
        
        uri = new HttpURI("/foo;abc=123/bar;jsessionid=12345#target");
        assertEquals("/foo;abc=123/bar;jsessionid=12345",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());
        assertEquals("jsessionid=12345",uri.getParam());
    }
    
    @Test
    public void testMutableURI()
    {
        HttpURI uri = new HttpURI("/foo/bar");
        assertEquals("/foo/bar",uri.toString());
        assertEquals("/foo/bar",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());

        uri.setScheme("http");
        assertEquals("http:/foo/bar",uri.toString());
        assertEquals("/foo/bar",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());

        uri.setAuthority("host",0);
        assertEquals("http://host/foo/bar",uri.toString());
        assertEquals("/foo/bar",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());

        uri.setAuthority("host",8888);
        assertEquals("http://host:8888/foo/bar",uri.toString());
        assertEquals("/foo/bar",uri.getPath());
        assertEquals("/foo/bar",uri.getDecodedPath());
        
        uri.setPathQuery("/f%30%30;p0/bar;p1;p2");
        assertEquals("http://host:8888/f%30%30;p0/bar;p1;p2",uri.toString());
        assertEquals("/f%30%30;p0/bar;p1;p2",uri.getPath());
        assertEquals("/f00/bar",uri.getDecodedPath());
        assertEquals("p2",uri.getParam());
        assertEquals(null,uri.getQuery());
        
        uri.setPathQuery("/f%30%30;p0/bar;p1;p2?name=value");
        assertEquals("http://host:8888/f%30%30;p0/bar;p1;p2?name=value",uri.toString());
        assertEquals("/f%30%30;p0/bar;p1;p2",uri.getPath());
        assertEquals("/f00/bar",uri.getDecodedPath());
        assertEquals("p2",uri.getParam());
        assertEquals("name=value",uri.getQuery());
        
        uri.setQuery("other=123456");
        assertEquals("http://host:8888/f%30%30;p0/bar;p1;p2?other=123456",uri.toString());
        assertEquals("/f%30%30;p0/bar;p1;p2",uri.getPath());
        assertEquals("/f00/bar",uri.getDecodedPath());
        assertEquals("p2",uri.getParam());
        assertEquals("other=123456",uri.getQuery());
    }

    @Test
    public void testSchemeAndOrAuthority() throws Exception
    {
        HttpURI uri = new HttpURI("/path/info");
        assertEquals("/path/info",uri.toString());
        
        uri.setAuthority("host",0);
        assertEquals("//host/path/info",uri.toString());
        
        uri.setAuthority("host",8888);
        assertEquals("//host:8888/path/info",uri.toString());
        
        uri.setScheme("http");
        assertEquals("http://host:8888/path/info",uri.toString());
        
        uri.setAuthority(null,0);
        assertEquals("http:/path/info",uri.toString());
        
    }
    
    @Test
    public void testBasicAuthCredentials() throws Exception
    {
        HttpURI uri = new HttpURI("http://user:password@example.com:8888/blah");
        assertEquals("http://user:password@example.com:8888/blah", uri.toString());
        assertEquals(uri.getAuthority(), "example.com:8888");
        assertEquals(uri.getUser(), "user:password");
    }
}
