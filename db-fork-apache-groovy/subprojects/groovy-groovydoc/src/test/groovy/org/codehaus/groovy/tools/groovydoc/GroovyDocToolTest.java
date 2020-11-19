/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.tools.groovydoc;

import groovy.util.GroovyTestCase;
import org.codehaus.groovy.groovydoc.GroovyClassDoc;
import org.codehaus.groovy.groovydoc.GroovyMethodDoc;
import org.codehaus.groovy.groovydoc.GroovyRootDoc;
import org.codehaus.groovy.tools.groovydoc.gstringTemplates.GroovyDocTemplateInfo;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyDocToolTest extends GroovyTestCase {
    private static final String MOCK_DIR = "mock/doc";
    private static final String TEMPLATES_DIR = "main/resources/org/codehaus/groovy/tools/groovydoc/gstringTemplates";

    GroovyDocTool xmlTool;
    GroovyDocTool xmlToolForTests;
    GroovyDocTool plainTool;
    GroovyDocTool htmlTool;

    public void setUp() {
        plainTool = new GroovyDocTool(new String[]{"src/test/groovy"});

        xmlTool = new GroovyDocTool(
                new FileSystemResourceManager("src"), // template storage
                new String[] {"src/main/java", "../../src/main/java", "src/test/groovy"}, // source file dirs
                new String[]{TEMPLATES_DIR + "/topLevel/rootDocStructuredData.xml"},
                new String[]{TEMPLATES_DIR + "/packageLevel/packageDocStructuredData.xml"},
                new String[]{TEMPLATES_DIR + "/classLevel/classDocStructuredData.xml"},
                new ArrayList<LinkArgument>(),
                new Properties()
        );

        xmlToolForTests = new GroovyDocTool(
                new FileSystemResourceManager("src"), // template storage
                new String[] {"src/test/groovy", "src/test/resources", "../../src/test"}, // source file dirs
                new String[]{TEMPLATES_DIR + "/topLevel/rootDocStructuredData.xml"},
                new String[]{TEMPLATES_DIR + "/packageLevel/packageDocStructuredData.xml"},
                new String[]{TEMPLATES_DIR + "/classLevel/classDocStructuredData.xml"},
                new ArrayList<LinkArgument>(),
                new Properties()
        );

        ArrayList<LinkArgument> links = new ArrayList<LinkArgument>();
        LinkArgument link = new LinkArgument();
        link.setHref("http://docs.oracle.com/javase/7/docs/api/");
        link.setPackages("java.,org.xml.,javax.,org.xml.");
        links.add(link);

        htmlTool = makeHtmltool(links, new Properties());
    }

    private GroovyDocTool makeHtmltool(ArrayList<LinkArgument> links, Properties props) {
        return new GroovyDocTool(
                new FileSystemResourceManager("src/main/resources"), // template storage
                new String[] {"src/test/groovy", "../../src/test"}, // source file dirs
                GroovyDocTemplateInfo.DEFAULT_DOC_TEMPLATES,
                GroovyDocTemplateInfo.DEFAULT_PACKAGE_TEMPLATES,
                GroovyDocTemplateInfo.DEFAULT_CLASS_TEMPLATES,
                links,
                props
        );
    }

    public void testPlainGroovyDocTool() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/GroovyDocToolTest.java");
        plainTool.add(srcList);
        GroovyRootDoc root = plainTool.getRootDoc();

        // loop through classes in tree
        GroovyClassDoc[] classDocs = root.classes();
        for (int i = 0; i < classDocs.length; i++) {
            GroovyClassDoc clazz = root.classes()[i];
            assertEquals("GroovyDocToolTest", clazz.name());

            // loop through methods in class
            boolean seenThisMethod = false;
            GroovyMethodDoc[] methodDocs = clazz.methods();
            for (int j = 0; j < methodDocs.length; j++) {
                GroovyMethodDoc method = clazz.methods()[j];
                if ("testPlainGroovyDocTool".equals(method.name())) {
                    seenThisMethod = true;
                    break;
                }
            }
            assertTrue(seenThisMethod);
        }
    }

    public void testGroovyDocTheCategoryMethodClass() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("groovy/util/CliBuilder.groovy");
        srcList.add("groovy/lang/GroovyLogTestCase.groovy");
        srcList.add("groovy/mock/interceptor/StrictExpectation.groovy");
        srcList.add("org/codehaus/groovy/runtime/GroovyCategorySupport.java");
        srcList.add("org/codehaus/groovy/runtime/ConvertedMap.java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);

        String groovyCategorySupportDocument = output.getText(MOCK_DIR + "/org/codehaus/groovy/runtime/GroovyCategorySupport.html");
        assertTrue(groovyCategorySupportDocument != null &&
                groovyCategorySupportDocument.indexOf("<method modifiers=\"public static \" returns=\"boolean\" name=\"hasCategoryInAnyThread\">") > 0);

        String categoryMethodDocument = output.getText(MOCK_DIR + "/org/codehaus/groovy/runtime/GroovyCategorySupport.CategoryMethodList.html");
        assertTrue(categoryMethodDocument != null &&
                categoryMethodDocument.indexOf("<method modifiers=\"public \" returns=\"boolean\" name=\"add\">") > 0);

        String packageDocument = output.getText(MOCK_DIR + "/org/codehaus/groovy/runtime/packageDocStructuredData.xml");
        assertTrue("Failed to find 'packageDocStructuredData.xml' in generated output", packageDocument != null);
        assertTrue(packageDocument.indexOf("<class name=\"GroovyCategorySupport\" />") > 0);
        assertTrue(packageDocument.indexOf("<class name=\"GroovyCategorySupport.CategoryMethod\" />") > 0);

        String rootDocument = output.getText(MOCK_DIR + "/rootDocStructuredData.xml");
        assertTrue("Failed to find 'rootDocStructuredData.xml' in generated output", rootDocument != null);
        assertTrue(rootDocument.indexOf("<package name=\"org/codehaus/groovy/runtime\" />") > 0);
        assertTrue(rootDocument.indexOf("<class path=\"org/codehaus/groovy/runtime/GroovyCategorySupport\" name=\"GroovyCategorySupport\" />") > 0);
        assertTrue(rootDocument.indexOf("<class path=\"org/codehaus/groovy/runtime/GroovyCategorySupport.CategoryMethod\" name=\"GroovyCategorySupport.CategoryMethod\" />") > 0);
    }

    public void testConstructors() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/TestConstructors";
        srcList.add(base + ".groovy");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String constructorDoc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, constructorDoc);
        assertTrue(constructorDoc.indexOf("<constructor modifiers=\"public \" name=\"TestConstructors\">") > 0);
        assertTrue(constructorDoc.indexOf("<parameter type=\"java.lang.ClassLoader\" name=\"parent\" />") > 0);
    }

    public void testClassComment() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/Builder";
        srcList.add(base + ".java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String builderDoc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, builderDoc);
        assertTrue(builderDoc,builderDoc.contains("A class comment"));
    }

    public void testMethodComment() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/ClassWithMethodComment.java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String defTabColDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/ClassWithMethodComment.html");
        assertTrue(defTabColDoc.contains("This is a method comment"));
    }

    public void testPackageName() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/Builder";
        srcList.add(base + ".java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String builderDoc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, builderDoc);
        assertTrue(builderDoc.contains("<containingPackage name=\"org/codehaus/groovy/tools/groovydoc/testfiles\">org.codehaus.groovy.tools.groovydoc.testfiles</containingPackage>"));
    }

    public void testExtendsClauseWithoutSuperClassInTree() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/Builder";
        srcList.add(base + ".java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String builderDoc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, builderDoc);
        assertTrue(builderDoc.contains("<extends>BuilderSupport</extends>"));
    }

    public void testExtendsClauseWithSuperClassInTree() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/Builder";
        srcList.add(base + ".java");
        srcList.add("groovy/util/BuilderSupport.java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String builderDoc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, builderDoc);
        assertTrue(builderDoc.contains("<extends>BuilderSupport</extends>"));
    }

    public void testInterfaceExtendsClauseWithMultipleInterfaces() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/GroovyInterfaceWithMultipleInterfaces.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/JavaInterfaceWithMultipleInterfaces.java");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/GroovyInterface1.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/JavaInterface1.java");
        xmlToolForTests.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlToolForTests.renderToOutput(output, MOCK_DIR);

        String groovyClassDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/GroovyInterfaceWithMultipleInterfaces.html");
        assertTrue(groovyClassDoc.indexOf("<interface>JavaInterface1</interface>") > 0);
        assertTrue(groovyClassDoc.indexOf("<interface>GroovyInterface1</interface>") > 0);
        assertTrue(groovyClassDoc.indexOf("<interface>Runnable</interface>") > 0);

        String javaClassDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/JavaInterfaceWithMultipleInterfaces.html");
        assertTrue(javaClassDoc.indexOf("<interface>JavaInterface1</interface>") > 0);
        assertTrue(javaClassDoc.indexOf("<interface>GroovyInterface1</interface>") > 0);
        assertTrue(javaClassDoc.indexOf("<interface>Runnable</interface>") > 0);
    }

    public void testImplementsClauseWithMultipleInterfaces() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/GroovyClassWithMultipleInterfaces.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/JavaClassWithMultipleInterfaces.java");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/GroovyInterface1.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/JavaInterface1.java");
        xmlToolForTests.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlToolForTests.renderToOutput(output, MOCK_DIR);

        String groovyClassDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/GroovyClassWithMultipleInterfaces.html");
        assertTrue(groovyClassDoc.indexOf("<interface>JavaInterface1</interface>") > 0);
        assertTrue(groovyClassDoc.indexOf("<interface>GroovyInterface1</interface>") > 0);
        assertTrue(groovyClassDoc.indexOf("<interface>Runnable</interface>") > 0);

        String javaClassDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/JavaClassWithMultipleInterfaces.html");
        assertTrue(javaClassDoc.indexOf("<interface>JavaInterface1</interface>") > 0);
        assertTrue(javaClassDoc.indexOf("<interface>GroovyInterface1</interface>") > 0);
        assertTrue(javaClassDoc.indexOf("<interface>Runnable</interface>") > 0);
    }

    public void testFullyQualifiedNamesInImplementsClause() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/GroovyClassWithMultipleInterfaces.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/JavaClassWithMultipleInterfaces.java");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/GroovyInterface1.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/JavaInterface1.java");
        xmlToolForTests.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlToolForTests.renderToOutput(output, MOCK_DIR);

        String groovyClassDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/GroovyClassWithMultipleInterfaces.html");
        assertTrue(groovyClassDoc.indexOf("<interface>GroovyInterface1</interface>") > 0);
        assertTrue(groovyClassDoc.indexOf("<interface>Runnable</interface>") > 0);

        String javaClassDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/JavaClassWithMultipleInterfaces.html");
        assertTrue(javaClassDoc.indexOf("<interface>JavaInterface1</interface>") > 0);
        assertTrue(javaClassDoc.indexOf("<interface>Runnable</interface>") > 0);
    }

    public void testDefaultPackage() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("DefaultPackageClassSupport.java");
        xmlToolForTests.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlToolForTests.renderToOutput(output, MOCK_DIR);
        String doc = output.getText(MOCK_DIR + "/DefaultPackage/DefaultPackageClassSupport.html");
        assertTrue(doc.indexOf("<extends>GroovyTestCase</extends>") > 0);
    }

    public void testJavaClassMultiCatch() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/MultiCatchExample";
        srcList.add(base + ".java");
        xmlToolForTests.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlToolForTests.renderToOutput(output, MOCK_DIR);
        String doc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, doc);
        assertTrue(doc, doc.contains("foo has a multi-catch exception inside"));
    }

    public void testStaticModifier() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/StaticModifier";
        srcList.add(base + ".java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String staticModifierDoc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, staticModifierDoc);
        assertTrue("static not found in: \"" + staticModifierDoc + "\"", staticModifierDoc.contains("static"));
    }

    public void testAnonymousInnerClassMethodsNotIncluded() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/ClassWithAnonymousInnerClass";
        srcList.add(base + ".java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String classWithAnonymousInnerClassDoc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, classWithAnonymousInnerClassDoc);
        assertTrue("innerClassMethod found in: \"" + classWithAnonymousInnerClassDoc + "\"", !classWithAnonymousInnerClassDoc.contains("innerClassMethod"));
    }

    public void testJavaClassWithDiamondOperator() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/JavaClassWithDiamond";
        srcList.add(base + ".java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String doc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for " + base, doc);
        assertTrue("stringList not found in: \"" + doc + "\"", doc.contains("stringList"));
    }

    public void testJavaStaticNestedClassWithDiamondOperator() throws Exception {
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/JavaStaticNestedClassWithDiamond";
        srcList.add(base + ".java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String doc = output.getText(MOCK_DIR + "/" + base + ".html");
        assertNotNull("No GroovyDoc found for outer class " + base, doc);
        assertTrue("Outer class expectedObject not found in: \"" + doc + "\"", doc.contains("expectedObject"));
        String docNested = output.getText(MOCK_DIR + "/" + base + ".Nested.html");
        assertNotNull("No GroovyDoc found for nested class " + base, docNested);
        assertTrue("Nested class comment not found in: \"" + docNested + "\"", docNested.contains("static nested class comment"));
    }

    public void testVisibilityPublic() throws Exception {
        Properties props = new Properties();
        props.put("publicScope", "true");
        testVisibility(props, true, false, false, false);
    }

    public void testVisibilityProtected() throws Exception {
        Properties props = new Properties();
        props.put("protectedScope", "true");
        testVisibility(props, true, true, false, false);
    }

    public void testVisibilityPackage() throws Exception {
        Properties props = new Properties();
        props.put("packageScope", "true");
        testVisibility(props, true, true, true, false);
    }

    public void testVisibilityPrivate() throws Exception {
        Properties props = new Properties();
        props.put("privateScope", "true");
        testVisibility(props, true, true, true, true);
    }

    public void testSinglePropertiesFromGetterSetter() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "str properties should be there", "<a href=\"#str\">str</a>", true);
    }

    public void testReOrderPropertiesFromGetterSetter() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "str1 properties should be there", "<a href=\"#str1\">str1</a>", true);
    }

    public void testCheckOtherTypesPropertiesFromGetterSetter() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "int properties should be there", "<a href=\"#int\">int</a>", true);
    }

    public void testPropertiesShouldNotBePresentForGetterAlone() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "shouldNotBePresent properties shouldn't be there", "<a href=\"#shouldNotBePresent\">shouldNotBePresent</a>", false);
    }

    public void testPropertiesPublicGetPrivateSet() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "_public_get_private_set shouldn't be present"
                    , "<a href=\"#_public_get_private_set\">_public_get_private_set</a>", false);
    }

    public void testPropertiesPrivateGetPublicSet() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "_private_get_public_set shouldn't be present",
                "<a href=\"#_private_get_public_set\">_private_get_public_set</a>", false);
    }

    public void testPropertiesPrivateGetPrivateSet() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "_private_get_private_set shouldn't be present",
                "<a href=\"#_private_get_private_set\">_private_get_private_set</a>", false);
    }

    public void testPropertiesShouldBePresentForSetIsBooleanType() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "testBoolean properties should be there", "<a href=\"#testBoolean\">testBoolean</a>", true);
    }

    public void testPropertiesShouldBePresentForIsSetBooleanType() throws Exception {
        testPropertiesFromGetterSetter("GeneratePropertyFromGetSet", "testBoolean2 properties should be there","<a href=\"#testBoolean2\">testBoolean2</a>", true);
    }

    private void testPropertiesFromGetterSetter(String fileName,String assertMessage,String expected,boolean isTrue) throws Exception {
        htmlTool = makeHtmltool(new ArrayList<LinkArgument>(), new Properties());
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/";
        srcList.add(base + fileName + ".groovy");
        htmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        String exampleClass = output.getText(MOCK_DIR + "/" + base + fileName + ".html");
        if (isTrue)
            assertTrue(assertMessage, exampleClass.contains(expected));
        else
            assertFalse(assertMessage,exampleClass.contains(expected));
    }

    private void testVisibility(Properties props, boolean a, boolean b, boolean c, boolean d) throws Exception {
        htmlTool = makeHtmltool(new ArrayList<LinkArgument>(), props);
        List<String> srcList = new ArrayList<String>();
        String base = "org/codehaus/groovy/tools/groovydoc/testfiles/ExampleVisibility";
        srcList.add(base + "G.groovy");
        srcList.add(base + "J.java");
        htmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        String javaExampleClass = output.getText(MOCK_DIR + "/" + base + "J.html");
        assertMethodVisibility(base, output, javaExampleClass, a, b, c, d);
        String groovyExampleClass = output.getText(MOCK_DIR + "/" + base + "G.html");
        assertMethodVisibility(base, output, groovyExampleClass, a, b, c, d);
    }

    private void assertMethodVisibility(String base, MockOutputTool output, String text, boolean a, boolean b, boolean c, boolean d) {
        assertNotNull("No GroovyDoc found for " + base + "\nFound: " + output, text);
        assertTrue("method a1" + (a ? " not" : "") + " found in: \"" + text + "\"", a ^ !text.contains("<a href=\"#a1()\">a1</a>"));
        assertTrue("method a2" + (a ? " not" : "") + " found in: \"" + text + "\"", a ^ !text.contains("<a href=\"#a2()\">a2</a>"));
        assertTrue("method b" + (b ? " not" : "") + " found in: \"" + text + "\"", b ^ !text.contains("<a href=\"#b()\">b</a>"));
        assertTrue("method c1" + (c ? " not" : "") + " found in: \"" + text + "\"", c ^ !text.contains("<a href=\"#c1()\">c1</a>"));
        assertTrue("method c2" + (c ? " not" : "") + " found in: \"" + text + "\"", c ^ !text.contains("<a href=\"#c2()\">c2</a>"));
        assertTrue("method d" + (d ? " not" : "") + " found in: \"" + text + "\"", d ^ !text.contains("<a href=\"#d()\">d</a>"));

        assertTrue("field _a" + (a ? " not" : "") + " found in: \"" + text + "\"", a ^ !text.contains("<a href=\"#_a\">_a</a>"));
        assertTrue("field _b" + (b ? " not" : "") + " found in: \"" + text + "\"", b ^ !text.contains("<a href=\"#_b\">_b</a>"));
        assertTrue("field _c" + (c ? " not" : "") + " found in: \"" + text + "\"", c ^ !text.contains("<a href=\"#_c\">_c</a>"));
        assertTrue("field _d" + (d ? " not" : "") + " found in: \"" + text + "\"", d ^ !text.contains("<a href=\"#_d\">_d</a>"));

        assertTrue("class A1" + (a ? " not" : "") + " found in: \"" + text + "\"", a ^ !text.contains(".A1</a></code>"));
        assertTrue("class A2" + (a ? " not" : "") + " found in: \"" + text + "\"", a ^ !text.contains(".A2</a></code>"));
        assertTrue("class B" + (b ? " not" : "") + " found in: \"" + text + "\"", b ^ !text.contains(".B</a></code>"));
        assertTrue("class C" + (c ? " not" : "") + " found in: \"" + text + "\"", c ^ !text.contains(".C</a></code>"));
        assertTrue("class D" + (d ? " not" : "") + " found in: \"" + text + "\"", d ^ !text.contains(".D</a></code>"));
    }

    public void testMultipleConstructorErrorBug() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/MultipleConstructorErrorBug.java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String sqlDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/MultipleConstructorErrorBug.html");
        System.out.println(sqlDoc);
        // VARBINARY() and other methods were assumed to be Constructors, make sure they aren't anymore...
        assertTrue(sqlDoc.indexOf("<method modifiers=\"public static \" returns=\"java.lang.String\" name=\"VARBINARY\">") > 0);
    }

    public void testReturnTypeResolution() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/SimpleGroovyRootDoc.java");
        srcList.add("org/codehaus/groovy/groovydoc/GroovyClassDoc.java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String text = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/SimpleGroovyRootDoc.html");
        assertTrue(text.indexOf("org.codehaus.groovy.groovydoc.GroovyClassDoc") > 0);
    }

    public void testParameterTypeResolution() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/SimpleGroovyRootDoc.java");
        srcList.add("org/codehaus/groovy/groovydoc/GroovyPackageDoc.java");
        xmlTool.add(srcList);
        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String text = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/SimpleGroovyRootDoc.html");
        assertTrue(text.indexOf("<parameter type=\"org.codehaus.groovy.groovydoc.GroovyPackageDoc\"") > 0);
    }

    public void testFileEncodingFallbackToCharset() throws Exception {
        String expectedCharset = "ISO-88591";

        Properties props = new Properties();
        props.setProperty("charset", expectedCharset);

        GroovyDocTool tool = new GroovyDocTool(
                new FileSystemResourceManager("src"),
                new String[0],
                new String[0],
                new String[0],
                new String[0],
                new ArrayList<LinkArgument>(),
                props);

        assertEquals("'fileEncoding' falls back to 'charset' if not provided", expectedCharset, tool.properties.getProperty("fileEncoding"));
    }

    public void testCharsetFallbackToFileEncoding() throws Exception {
        String expectedCharset = "ISO-88591";

        Properties props = new Properties();
        props.setProperty("fileEncoding", expectedCharset);

        GroovyDocTool tool = new GroovyDocTool(
                new FileSystemResourceManager("src"),
                new String[0],
                new String[0],
                new String[0],
                new String[0],
                new ArrayList<LinkArgument>(),
                props);

        assertEquals("'charset' falls back to 'fileEncoding' if not provided", expectedCharset, tool.properties.getProperty("charset"));

    }

    public void testFileEncodingCharsetFallbackToDefaultCharset() throws Exception {
        String expectedCharset = Charset.defaultCharset().name();

        GroovyDocTool tool = new GroovyDocTool(
                new FileSystemResourceManager("src"),
                new String[0],
                new String[0],
                new String[0],
                new String[0],
                new ArrayList<LinkArgument>(),
                new Properties());

        assertEquals("'charset' falls back to the default charset", expectedCharset, tool.properties.getProperty("charset"));
        assertEquals("'fileEncoding' falls back to the default charset", expectedCharset, tool.properties.getProperty("fileEncoding"));
    }

    // GROOVY-5940
    public void testWrongPackageNameInClassHierarchyWithPlainTool() throws Exception {
        List<String> srcList = new ArrayList<String>();

        String fullPathBaseA = "org/codehaus/groovy/tools/groovydoc/testfiles/a/Base";
        srcList.add(fullPathBaseA + ".groovy");

        String fullPathBaseB = "org/codehaus/groovy/tools/groovydoc/testfiles/b/Base";
        srcList.add(fullPathBaseB + ".groovy");

        String fullPathBaseC = "org/codehaus/groovy/tools/groovydoc/testfiles/c/Base";

        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/a/DescendantA.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/b/DescendantB.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/a/DescendantC.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/a/DescendantD.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/c/DescendantE.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/c/DescendantF.groovy");

        plainTool.add(srcList);

        GroovyRootDoc root = plainTool.getRootDoc();

        // loop through classes in tree
        GroovyClassDoc classDocDescendantA = getGroovyClassDocByName(root, "DescendantA");
        assertTrue(fullPathBaseA.equals(root.classNamed(classDocDescendantA, "Base").getFullPathName()));

        GroovyClassDoc classDocDescendantB = getGroovyClassDocByName(root, "DescendantB");
        assertTrue(fullPathBaseB.equals(root.classNamed(classDocDescendantB, "Base").getFullPathName()));

        GroovyClassDoc classDocDescendantC = getGroovyClassDocByName(root, "DescendantC");
        assertTrue(fullPathBaseA.equals(root.classNamed(classDocDescendantC, "Base").getFullPathName()));

        GroovyClassDoc classDocDescendantD = getGroovyClassDocByName(root, "DescendantD");
        assertTrue(fullPathBaseA.equals(root.classNamed(classDocDescendantD, "Base").getFullPathName()));

        GroovyClassDoc classDocDescendantE = getGroovyClassDocByName(root, "DescendantE");
        assertTrue(fullPathBaseC.equals(root.classNamed(classDocDescendantE, "Base").getFullPathName()));

        GroovyClassDoc classDocDescendantF = getGroovyClassDocByName(root, "DescendantF");
        assertTrue(fullPathBaseC.equals(root.classNamed(classDocDescendantF, "Base").getFullPathName()));
    }

    // GROOVY-5939
    public void testArrayPropertyLinkWithSelfReference() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/ArrayPropertyLink.groovy");
        htmlTool.add(srcList);

        MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        String arrayPropertyLinkDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/ArrayPropertyLink.html");

        Pattern p = Pattern.compile("<a(.+?)ArrayPropertyLink.html'>(.+?)</a>\\[\\]");
        Matcher m = p.matcher(arrayPropertyLinkDoc);

        assertTrue(m.find());
        assertEquals("There has to be at least a single reference to the ArrayPropertyLink[]", "ArrayPropertyLink", m.group(2));
    }

    public void testClassesAreNotInitialized() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/staticInit/UsesClassesWithFailingStaticInit.groovy");
        htmlTool.add(srcList);

        MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        String doc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/staticInit/UsesClassesWithFailingStaticInit.html");

        assertTrue(doc.contains("org.codehaus.groovy.tools.groovydoc.testfiles.staticInit.JavaWithFailingStaticInit"));
    }

    public void testArrayPropertyLinkWithExternalReference() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/PropertyLink.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/ArrayPropertyLink.groovy");
        htmlTool.add(srcList);

        MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        String propertyLinkDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/PropertyLink.html");

        Pattern p = Pattern.compile("<a(.+?)ArrayPropertyLink.html'>(.+?)</a>\\[\\]");
        Matcher m = p.matcher(propertyLinkDoc);

        assertTrue(m.find());
        assertEquals("There has to be at least a single reference to the ArrayPropertyLink[]", "ArrayPropertyLink", m.group(2));
    }

    public void testInnerEnumReference() throws Exception {
        List<String> srcList = new ArrayList<String>();

        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/InnerEnum.groovy");
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/InnerClassProperty.groovy");
        htmlTool.add(srcList);

        MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        String derivDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/InnerClassProperty.html");

        Pattern p = Pattern.compile("<a(.+?)testfiles/InnerEnum.Enum.html'>(.+?)</a>");
        Matcher m = p.matcher(derivDoc);

        assertTrue(m.find());
        assertEquals("There has to be a reference to class Enum", "Enum", m.group(2));
    }

    public void testClassAliasing() throws Exception {

        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/Alias.groovy");
        htmlTool.add(srcList);

        MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        String derivDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/Alias.html");

        Pattern p = Pattern.compile("<a(.+?)java/util/ArrayList.html' title='ArrayList'>(.+?)</a>");
        Matcher m = p.matcher(derivDoc);

        assertTrue(m.find());
        assertEquals("There has to be a reference to class ArrayList", "ArrayList", m.group(2));
    }

    public void testImplementedInterfaceWithAlias() throws Exception {
        // FooAdapter imports both api.Foo and lib.Foo, using "lib.Foo as FooImpl" to disambiguate.
        // lib.Foo is imported later that api.Foo, so groovydoc tries to resolve to lib.Foo first.
        htmlTool.add(Arrays.asList(
                "org/codehaus/groovy/tools/groovydoc/testfiles/alias/api/Foo.java",
                "org/codehaus/groovy/tools/groovydoc/testfiles/alias/lib/Foo.java",
                "org/codehaus/groovy/tools/groovydoc/testfiles/alias/FooAdapter.groovy"
        ));

        final MockOutputTool output = new MockOutputTool();
        htmlTool.renderToOutput(output, MOCK_DIR);
        final String fooAdapterDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/alias/FooAdapter.html");

        // "Interfaces and Traits" section should show "Foo" as one of the implemented interfaces,
        // and that should link to api/Foo.html, not to lib/Foo.html.
        final Matcher interfacesAndTraits = Pattern.compile(
                "<dt>All Implemented Interfaces and Traits:</dt>\\s*" +
                "<dd><a href='[./]*/org/codehaus/groovy/tools/groovydoc/testfiles/alias/(api|lib)/Foo\\.html'>(Foo|FooImpl)</a></dd>"
        ).matcher(fooAdapterDoc);

        // Constructor is actually "FooAdapter(FooImpl foo)",
        // but it should show "Foo" as the link text, not "FooImpl".
        // The Foo parameter type should link to lib/Foo.html, not api/Foo.html.
        final Matcher constructor = Pattern.compile(
                "FooAdapter(</[a-z]+>)*\\(<a href='[./]*/org/codehaus/groovy/tools/groovydoc/testfiles/alias/(api|lib)/Foo.html'>(Foo|FooImpl)</a> foo\\)"
        ).matcher(fooAdapterDoc);

        assertTrue("Interfaces and Traits pattern should match for this test to make sense", interfacesAndTraits.find());
        assertTrue("Constructor pattern should match for this test to make sense", constructor.find());

        assertEquals("The implemented interface should link to api.Foo", "api", interfacesAndTraits.group(1));
        assertEquals("The implemented interface link text should be Foo", "Foo", interfacesAndTraits.group(2));
        assertEquals("The constructor parameter should link to lib.Foo", "lib", constructor.group(2));
        assertEquals("The constructor parameter link text should be Foo", "Foo", constructor.group(3));
    }

    public void testScript() throws Exception {
        List<String> srcList = new ArrayList<String>();
        srcList.add("org/codehaus/groovy/tools/groovydoc/testfiles/Script.groovy");
        xmlTool.add(srcList);

        MockOutputTool output = new MockOutputTool();
        xmlTool.renderToOutput(output, MOCK_DIR);
        String scriptDoc = output.getText(MOCK_DIR + "/org/codehaus/groovy/tools/groovydoc/testfiles/Script.html");
        assertTrue("There should be a reference to method sayHello", containsTagWithName(scriptDoc, "method", "sayHello"));
        assertTrue(scriptDoc, scriptDoc.contains("Use this to say Hello"));

        assertTrue("There should be a reference to method sayGoodbye", containsTagWithName(scriptDoc, "method", "sayGoodbye"));
        assertTrue(scriptDoc, scriptDoc.contains("Use this to bid farewell"));

        assertTrue("There should be a reference to property instanceProp", containsTagWithName(scriptDoc, "property", "instanceProp"));

        assertTrue("There should be a reference to field staticField", containsTagWithName(scriptDoc, "field", "staticField"));

        assertFalse("Script local variables should not appear in groovydoc output", scriptDoc.contains("localVar"));
    }

    private boolean containsTagWithName(String text, String tagname, String name) {
        return text.matches("(?s).*<"+ tagname + "[^>]* name=\""+ name + "\".*");
    }

    private GroovyClassDoc getGroovyClassDocByName(GroovyRootDoc root, String name) {
        GroovyClassDoc[] classes = root.classes();

        for (GroovyClassDoc clazz : classes) {
            if (clazz.getFullPathName().endsWith(name)) {
                return clazz;
            }
        }

        return null;
    }
}
