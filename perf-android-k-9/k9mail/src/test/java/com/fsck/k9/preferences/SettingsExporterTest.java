package com.fsck.k9.preferences;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class SettingsExporterTest {

    @Test
    public void exportPreferences_producesXML() throws Exception {
        Document document = exportPreferences(false, Collections.<String>emptySet());

        assertEquals("k9settings", document.getRootElement().getName());
    }

    @Test
    public void exportPreferences_setsVersionTo43() throws Exception {
        Document document = exportPreferences(false, Collections.<String>emptySet());

        assertEquals("43", document.getRootElement().getAttributeValue("version"));
    }

    @Test
    public void exportPreferences_setsFormatTo1() throws Exception {
        Document document = exportPreferences(false, Collections.<String>emptySet());

        assertEquals("1", document.getRootElement().getAttributeValue("format"));
    }

    @Test
    public void exportPreferences_exportsGlobalSettingsWhenRequested() throws Exception {
        Document document = exportPreferences(true, Collections.<String>emptySet());

        assertNotNull(document.getRootElement().getChild("global"));
    }

    @Test
    public void exportPreferences_ignoresGlobalSettingsWhenRequested() throws Exception {
        Document document = exportPreferences(false, Collections.<String>emptySet());

        assertNull(document.getRootElement().getChild("global"));
    }

    private Document exportPreferences(boolean globalSettings, Set<String> accounts) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SettingsExporter.exportPreferences(RuntimeEnvironment.application, outputStream,
                globalSettings, accounts);
        Document document = parseXML(outputStream.toByteArray());
        outputStream.close();
        return document;
    }

    private Document parseXML(byte[] xml) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        InputStream stream = new ByteArrayInputStream(xml);
        return builder.build(stream);
    }
}
