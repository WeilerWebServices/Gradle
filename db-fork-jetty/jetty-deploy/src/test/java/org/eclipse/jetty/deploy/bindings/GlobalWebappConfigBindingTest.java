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

package org.eclipse.jetty.deploy.bindings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;

import org.eclipse.jetty.deploy.providers.ScanningAppProvider;
import org.eclipse.jetty.deploy.test.XmlConfiguredJetty;
import org.eclipse.jetty.toolchain.test.IO;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.PathAssert;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDir;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDirExtension;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link ScanningAppProvider} as it starts up for the first time.
 */
@ExtendWith(WorkDirExtension.class)
public class GlobalWebappConfigBindingTest
{
    public WorkDir testdir;
    private static XmlConfiguredJetty jetty;

    @BeforeEach
    public void setupEnvironment() throws Exception
    {
        jetty = new XmlConfiguredJetty(testdir.getEmptyPathDir());
        jetty.addConfiguration("jetty.xml");
        jetty.addConfiguration("jetty-http.xml");

        // Setup initial context
        jetty.copyWebapp("foo.xml","foo.xml");
        jetty.copyWebapp("foo-webapp-1.war","foo.war");

    }

    @AfterEach
    public void teardownEnvironment() throws Exception
    {
        // Stop jetty.
        jetty.stop();
    }

    @Test
    public void testServerAndSystemClassesOverride() throws Exception
    {
        File srcXml = MavenTestingUtils.getTestResourceFile("context-binding-test-1.xml");
        File destXml = new File(jetty.getJettyHome(),"context-binding-test-1.xml");
        IO.copy(srcXml,destXml);

        PathAssert.assertFileExists("Context Binding XML",destXml);

        jetty.addConfiguration("binding-test-contexts-1.xml");
        jetty.load();
        jetty.start();

        List<WebAppContext> contexts = jetty.getWebAppContexts();
        assertThat("List of Contexts", contexts, hasSize(greaterThan(0)));

        WebAppContext context = contexts.get(0);

        assertNotNull(context, "Context should not be null");
        String defaultClasses[] = context.getDefaultServerClasses();
        String currentClasses[] = context.getServerClasses();

        String addedClass = "org.eclipse.foo."; // What was added by the binding
        assertThat("Default Server Classes",addedClass,not(isIn(defaultClasses)));
        assertThat("Current Server Classes",addedClass,isIn(currentClasses));

        //  boolean jndiPackage = false;

        // this test overrides and we removed the jndi from the list so it
        // should test false
        //        for (String entry : context.getSystemClasses())
        //        {
        //            if ("org.eclipse.jetty.jndi.".equals(entry))
        //            {
        //                jndiPackage = true;
        //            }
        //        }
        //
        //        assertFalse(jndiPackage);
    }
}
