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

package org.eclipse.jetty.osgi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.ArrayList;

import javax.inject.Inject;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * TestJettyOSGiBootContextAsService
 * 
 * Tests deployment of a ContextHandler as an osgi Service.
 * 
 * Tests the ServiceContextProvider.
 * 
 */
@RunWith(PaxExam.class)
public class TestJettyOSGiBootContextAsService
{
    private static final String LOG_LEVEL = "WARN";


    @Inject
    BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure()
    {
        ArrayList<Option> options = new ArrayList<>();
        options.add(CoreOptions.junitBundles());
        options.addAll(TestOSGiUtil.configureJettyHomeAndPort(false, "jetty-http-boot-context-as-service.xml"));
        options.add(CoreOptions.bootDelegationPackages("org.xml.sax", "org.xml.*", "org.w3c.*", "javax.xml.*"));
        options.addAll(TestOSGiUtil.coreJettyDependencies());

        // a bundle that registers a webapp as a service for the jetty osgi core
        // to pick up and deploy
        options.add(mavenBundle().groupId("org.eclipse.jetty.osgi").artifactId("test-jetty-osgi-context").versionAsInProject().start());
        options.add(systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value(LOG_LEVEL));
        options.add(systemProperty("org.eclipse.jetty.LEVEL").value(LOG_LEVEL));
        options.add( systemProperty( "org.ops4j.pax.url.mvn.localRepository" ).value( System.getProperty( "mavenRepoPath" ) ) );
        
        return options.toArray(new Option[options.size()]);
    }



    /**
     */
    @Test
    public void testContextHandlerAsOSGiService() throws Exception
    {
        if (Boolean.getBoolean(TestOSGiUtil.BUNDLE_DEBUG))
            TestOSGiUtil.assertAllBundlesActiveOrResolved(bundleContext);
        
        // now test the context
        HttpClient client = new HttpClient();
        try
        {
            client.start();
            String tmp = System.getProperty("boot.context.service.port");
            assertNotNull(tmp);
            int port = Integer.valueOf(tmp).intValue();
            ContentResponse response = client.GET("http://127.0.0.1:" + port + "/acme/index.html");
            assertEquals(HttpStatus.OK_200, response.getStatus());

            String content = new String(response.getContent());
            assertTrue(content.indexOf("<h1>Test OSGi Context</h1>") != -1);
        }
        finally
        {
            client.stop();
        }

        ServiceReference[] refs = bundleContext.getServiceReferences(ContextHandler.class.getName(), null);
        assertNotNull(refs);
        assertEquals(1, refs.length);
        ContextHandler ch = (ContextHandler) bundleContext.getService(refs[0]);
        assertEquals("/acme", ch.getContextPath());

        // Stop the bundle with the ContextHandler in it and check the jetty
        // Context is destroyed for it.
        // TODO: think of a better way to communicate this to the test, other
        // than checking stderr output
        Bundle testWebBundle = TestOSGiUtil.getBundle(bundleContext, "org.eclipse.jetty.osgi.testcontext");
        assertNotNull("Could not find the org.eclipse.jetty.test-jetty-osgi-context.jar bundle", testWebBundle);
        assertTrue("The bundle org.eclipse.jetty.testcontext is not correctly resolved", testWebBundle.getState() == Bundle.ACTIVE);
        testWebBundle.stop();
    }
}
