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

package org.eclipse.jetty.websocket.common.test;

import org.eclipse.jetty.toolchain.test.matchers.RegexMatcher;

public class MoreMatchers
{
    /**
     * Create a matcher for {@link String} that matches against a regex pattern.
     *
     * <p>
     *     Returns success based on {@code java.util.regex.Pattern.matcher(input).matches();}
     * </p>
     *
     * @param pattern the {@link java.util.regex.Pattern} syntax pattern to match against.
     * @return the Regex Matcher
     */
    public static org.hamcrest.Matcher<java.lang.String> regex(String pattern)
    {
        return new RegexMatcher(pattern);
    }
}
