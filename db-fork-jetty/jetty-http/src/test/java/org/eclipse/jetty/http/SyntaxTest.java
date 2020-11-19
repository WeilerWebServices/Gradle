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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class SyntaxTest
{
    @Test
    public void testRequireValidRFC2616Token_Good()
    {
        String tokens[] = {
                "name",
                "",
                null,
                "n.a.m.e",
                "na-me",
                "+name",
                "na*me",
                "na$me",
                "#name"
        };
        
        for (String token : tokens)
        {
            Syntax.requireValidRFC2616Token(token, "Test Based");
            // No exception should occur here
        }
    }
    
    @Test
    public void testRequireValidRFC2616Token_Bad()
    {
        String tokens[] = {
                "\"name\"",
                "name\t",
                "na me",
                "name\u0082",
                "na\tme",
                "na;me",
                "{name}",
                "[name]",
                "\""
        };
        
        for (String token : tokens)
        {
            try
            {
                Syntax.requireValidRFC2616Token(token, "Test Based");
                fail("RFC2616 Token [" + token + "] Should have thrown " + IllegalArgumentException.class.getName());
            }
            catch (IllegalArgumentException e)
            {
                assertThat("Testing Bad RFC2616 Token [" + token + "]", e.getMessage(),
                        allOf(containsString("Test Based"),
                                containsString("RFC2616")));
            }
        }
    }
    
    @Test
    public void testRequireValidRFC6265CookieValue_Good()
    {
        String values[] = {
                "value",
                "",
                null,
                "val=ue",
                "val-ue",
                "\"value\"",
                "val/ue",
                "v.a.l.u.e"
        };
        
        for (String value : values)
        {
            Syntax.requireValidRFC6265CookieValue(value);
            // No exception should occur here
        }
    }
    
    @Test
    public void testRequireValidRFC6265CookieValue_Bad()
    {
        String values[] = {
                "va\tlue",
                "\t",
                "value\u0000",
                "val\u0082ue",
                "va lue",
                "va;lue",
                "\"value",
                "value\"",
                "val\\ue",
                "val\"ue",
                "\""
        };
        
        for (String value : values)
        {
            try
            {
                Syntax.requireValidRFC6265CookieValue(value);
                fail("RFC6265 Cookie Value [" + value + "] Should have thrown " + IllegalArgumentException.class.getName());
            }
            catch (IllegalArgumentException e)
            {
                assertThat("Testing Bad RFC6265 Cookie Value [" + value + "]", e.getMessage(), containsString("RFC6265"));
            }
        }
    }
}
